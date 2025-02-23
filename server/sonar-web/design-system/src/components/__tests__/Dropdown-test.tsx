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
import { screen } from '@testing-library/react';
import { renderWithRouter } from '../../helpers/testUtils';
import { ButtonSecondary } from '../buttons';
import Dropdown, { ActionsDropdown } from '../Dropdown';

describe('Dropdown with Portal Wrapper', () => {
  it('renders', async () => {
    const { user } = setupWithChildren();
    expect(screen.getByRole('button')).toBeInTheDocument();

    await user.click(screen.getByRole('button'));
    expect(screen.getByRole('menu')).toBeInTheDocument();
  });

  it('toggles with render prop', async () => {
    const { user } = setupWithChildren(({ onToggleClick }) => (
      <ButtonSecondary onClick={onToggleClick} />
    ));

    await user.click(screen.getByRole('button'));
    expect(screen.getByRole('menu')).toBeVisible();
  });

  function setupWithChildren(children?: Dropdown['props']['children']) {
    return renderWithRouter(
      <Dropdown id="test-menu" isPortal={true} overlay={<div id="overlay" />}>
        {children ?? <ButtonSecondary />}
      </Dropdown>
    );
  }
});

describe('Dropdown', () => {
  it('renders', async () => {
    const { user } = setupWithChildren();
    expect(screen.getByRole('button')).toBeInTheDocument();

    await user.click(screen.getByRole('button'));
    expect(screen.getByRole('menu')).toBeInTheDocument();
  });

  it('toggles with render prop', async () => {
    const { user } = setupWithChildren(({ onToggleClick }) => (
      <ButtonSecondary onClick={onToggleClick} />
    ));

    await user.click(screen.getByRole('button'));
    expect(screen.getByRole('menu')).toBeVisible();
  });

  function setupWithChildren(children?: Dropdown['props']['children']) {
    return renderWithRouter(
      <Dropdown id="test-menu" overlay={<div id="overlay" />}>
        {children ?? <ButtonSecondary />}
      </Dropdown>
    );
  }
});

describe('ActionsDropdown', () => {
  it('renders', () => {
    setup();
    expect(screen.getByRole('button')).toHaveAccessibleName('menu');
  });

  function setup() {
    return renderWithRouter(
      <ActionsDropdown id="test-menu">
        <div id="overlay" />
      </ActionsDropdown>
    );
  }
});
