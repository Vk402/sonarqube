/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.permission.ws;

import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.web.UserRole;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;

import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;

public class GroupsActionIT extends BasePermissionWsIT<GroupsAction> {

  private GroupDto group1;
  private GroupDto group2;
  private final ResourceTypes resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private final PermissionService permissionService = new PermissionServiceImpl(resourceTypes);
  private final WsParameters wsParameters = new WsParameters(permissionService);
  private final ManagedInstanceService managedInstanceService = mock(ManagedInstanceService.class);

  @Override
  protected GroupsAction buildWsAction() {
    return new GroupsAction(
      db.getDbClient(),
      userSession,
      newPermissionWsSupport(), wsParameters, managedInstanceService);
  }

  @Before
  public void setUp() {
    group1 = db.users().insertGroup("group-1-name");
    group2 = db.users().insertGroup("group-2-name");
    GroupDto group3 = db.users().insertGroup("group-3-name");
    db.users().insertPermissionOnGroup(group1, GlobalPermission.SCAN);
    db.users().insertPermissionOnGroup(group2, GlobalPermission.SCAN);
    db.users().insertPermissionOnGroup(group3, GlobalPermission.ADMINISTER);
    db.users().insertPermissionOnAnyone(GlobalPermission.SCAN);
    db.commit();
  }

  @Test
  public void verify_definition() {
    Action wsDef = wsTester.getDef();

    assertThat(wsDef.isInternal()).isTrue();
    assertThat(wsDef.since()).isEqualTo("5.2");
    assertThat(wsDef.isPost()).isFalse();
    assertThat(wsDef.changelog()).extracting(Change::getVersion, Change::getDescription).containsExactlyInAnyOrder(
      tuple("10.0", "Response includes 'managed' field."),
      tuple("8.4", "Field 'id' in the response is deprecated. Format changes from integer to string."),
      tuple("7.4", "The response list is returning all groups even those without permissions, the groups with permission are at the top of the list."));
  }

  @Test
  public void search_for_groups_with_one_permission() {
    loginAsAdmin();

    String json = newRequest()
      .setParam(PARAM_PERMISSION, GlobalPermission.SCAN.getKey())
      .execute()
      .getInput();
    assertJson(json).isSimilarTo("{\n" +
      "  \"paging\": {\n" +
      "    \"pageIndex\": 1,\n" +
      "    \"pageSize\": 20,\n" +
      "    \"total\": 3\n" +
      "  },\n" +
      "  \"groups\": [\n" +
      "    {\n" +
      "      \"name\": \"Anyone\",\n" +
      "      \"permissions\": [\n" +
      "        \"scan\"\n" +
      "      ]\n" +
      "    },\n" +
      "    {\n" +
      "      \"name\": \"group-1-name\",\n" +
      "      \"description\": \"" + group1.getDescription() + "\",\n" +
      "      \"permissions\": [\n" +
      "        \"scan\"\n" +
      "      ],\n" +
      "      \"managed\": false\n" +
      "    },\n" +
      "    {\n" +
      "      \"name\": \"group-2-name\",\n" +
      "      \"description\": \"" + group2.getDescription() + "\",\n" +
      "      \"permissions\": [\n" +
      "        \"scan\"\n" +
      "      ],\n" +
      "      \"managed\": false\n" +
      "    }\n" +
      "  ]\n" +
      "}\n");
  }

  @Test
  public void search_with_selection() {
    loginAsAdmin();
    String result = newRequest()
      .setParam(PARAM_PERMISSION, GlobalPermission.SCAN.getKey())
      .execute()
      .getInput();

    assertThat(result).containsSubsequence(DefaultGroups.ANYONE, "group-1", "group-2");
  }

  @Test
  public void search_groups_with_pagination() {
    loginAsAdmin();
    String result = newRequest()
      .setParam(PARAM_PERMISSION, GlobalPermission.SCAN.getKey())
      .setParam(PAGE_SIZE, "1")
      .setParam(PAGE, "3")
      .execute()
      .getInput();

    assertThat(result).contains("group-2")
      .doesNotContain("group-1")
      .doesNotContain("group-3");
  }

  @Test
  public void search_groups_with_query() {
    loginAsAdmin();
    String result = newRequest()
      .setParam(PARAM_PERMISSION, GlobalPermission.SCAN.getKey())
      .setParam(TEXT_QUERY, "group-")
      .execute()
      .getInput();

    assertThat(result)
      .contains("group-1", "group-2")
      .doesNotContain(DefaultGroups.ANYONE);
  }

  @Test
  public void search_groups_with_project_permissions() {
    ComponentDto project = db.components().insertPrivateProject();
    GroupDto group = db.users().insertGroup("project-group-name");
    db.users().insertProjectPermissionOnGroup(group, ISSUE_ADMIN, project);

    ComponentDto anotherProject = db.components().insertPrivateProject();
    GroupDto anotherGroup = db.users().insertGroup("another-project-group-name");
    db.users().insertProjectPermissionOnGroup(anotherGroup, ISSUE_ADMIN, anotherProject);

    GroupDto groupWithoutPermission = db.users().insertGroup("group-without-permission");

    userSession.logIn().addProjectPermission(UserRole.ADMIN, project);
    String result = newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .execute()
      .getInput();

    assertThat(result).contains(group.getName())
      .doesNotContain(anotherGroup.getName())
      .doesNotContain(groupWithoutPermission.getName());
  }

  @Test
  public void return_also_groups_without_permission_when_search_query() {
    ComponentDto project = db.components().insertPrivateProject();
    GroupDto group = db.users().insertGroup("group-with-permission");
    db.users().insertProjectPermissionOnGroup(group, ISSUE_ADMIN, project);

    GroupDto groupWithoutPermission = db.users().insertGroup("group-without-permission");
    GroupDto anotherGroup = db.users().insertGroup("another-group");

    loginAsAdmin();
    String result = newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(TEXT_QUERY, "group-with")
      .execute()
      .getInput();

    assertThat(result).contains(group.getName())
      .doesNotContain(groupWithoutPermission.getName())
      .doesNotContain(anotherGroup.getName());
  }

  @Test
  public void return_only_groups_with_permission_when_no_search_query() {
    ComponentDto project = db.components().insertComponent(newPrivateProjectDto("project-uuid"));
    GroupDto group = db.users().insertGroup("project-group-name");
    db.users().insertProjectPermissionOnGroup(group, ISSUE_ADMIN, project);

    GroupDto groupWithoutPermission = db.users().insertGroup("group-without-permission");

    loginAsAdmin();
    String result = newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .execute()
      .getInput();

    assertThat(result).contains(group.getName())
      .doesNotContain(groupWithoutPermission.getName());
  }

  @Test
  public void return_anyone_group_when_search_query_and_no_param_permission() {
    ComponentDto project = db.components().insertPrivateProject();
    GroupDto group = db.users().insertGroup("group-with-permission");
    db.users().insertProjectPermissionOnGroup(group, ISSUE_ADMIN, project);

    loginAsAdmin();
    String result = newRequest()
      .setParam(PARAM_PROJECT_ID, project.uuid())
      .setParam(TEXT_QUERY, "nyo")
      .execute()
      .getInput();

    assertThat(result).contains("Anyone");
  }

  @Test
  public void search_groups_on_views() {
    ComponentDto view = db.components().insertComponent(ComponentTesting.newPortfolio("view-uuid").setKey("view-key"));
    GroupDto group = db.users().insertGroup("project-group-name");
    db.users().insertProjectPermissionOnGroup(group, ISSUE_ADMIN, view);

    loginAsAdmin();
    String result = newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .setParam(PARAM_PROJECT_ID, "view-uuid")
      .execute()
      .getInput();

    assertThat(result).contains("project-group-name")
      .doesNotContain("group-1")
      .doesNotContain("group-2")
      .doesNotContain("group-3");
  }

  @Test
  public void return_isManaged() {
    ComponentDto view = db.components().insertComponent(ComponentTesting.newPortfolio("view-uuid").setKey("view-key"));
    GroupDto managedGroup = db.users().insertGroup("managed-group");
    GroupDto localGroup = db.users().insertGroup("local-group");
    db.users().insertProjectPermissionOnGroup(managedGroup, ISSUE_ADMIN, view);
    db.users().insertProjectPermissionOnGroup(localGroup, ISSUE_ADMIN, view);
    mockGroupsAsManaged(managedGroup.getUuid());

    loginAsAdmin();
    String result = newRequest()
      .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
      .setParam(PARAM_PROJECT_ID, "view-uuid")
      .execute()
      .getInput();

    assertJson(result).isSimilarTo(
      "{\n"
        + "  \"paging\": {\n"
        + "    \"pageIndex\": 1,\n"
        + "    \"pageSize\": 20,\n"
        + "    \"total\": 2\n"
        + "  },\n"
        + "  \"groups\": [\n"
        + "    {\n"
        + "      \"name\": \"local-group\",\n"
        + "      \"managed\": false\n"
        + "    },\n"
        + "    {\n"
        + "      \"name\": \"managed-group\",\n"
        + "      \"managed\": true\n"
        + "    }\n"
        + "  ]\n"
        + "}"
    );
  }

  @Test
  public void fail_if_not_logged_in() {
    assertThatThrownBy(() -> {
      userSession.anonymous();

      newRequest()
        .setParam(PARAM_PERMISSION, GlobalPermission.SCAN.getKey())
        .execute();
    })
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_if_insufficient_privileges() {
    assertThatThrownBy(() -> {
      userSession.logIn("login");
      newRequest()
        .setParam(PARAM_PERMISSION, GlobalPermission.SCAN.getKey())
        .execute();
    })
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_if_project_uuid_and_project_key_are_provided() {
    ComponentDto project = db.components().insertPrivateProject();

    assertThatThrownBy(() -> {
      loginAsAdmin();
      newRequest()
        .setParam(PARAM_PERMISSION, GlobalPermission.SCAN.getKey())
        .setParam(PARAM_PROJECT_ID, project.uuid())
        .setParam(PARAM_PROJECT_KEY, project.getKey())
        .execute();
    })
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void fail_when_using_branch_uuid() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto branch = db.components().insertProjectBranch(project);
    GroupDto group = db.users().insertGroup();
    db.users().insertProjectPermissionOnGroup(group, ISSUE_ADMIN, project);
    loginAsAdmin();

    assertThatThrownBy(() -> {
      newRequest()
        .setParam(PARAM_PERMISSION, ISSUE_ADMIN)
        .setParam(PARAM_PROJECT_ID, branch.uuid())
        .execute();
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Project id '%s' not found", branch.uuid()));
  }

  private void mockGroupsAsManaged(String... groupUuids) {
    when(managedInstanceService.getGroupUuidToManaged(any(), any())).thenAnswer(invocation ->
      {
        Set<?> allGroupUuids = invocation.getArgument(1, Set.class);
        return allGroupUuids.stream()
          .map(groupUuid -> (String) groupUuid)
          .collect(toMap(identity(), userUuid -> Set.of(groupUuids).contains(userUuid)));
      }
    );
  }
}
