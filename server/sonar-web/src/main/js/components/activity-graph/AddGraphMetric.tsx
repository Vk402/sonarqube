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
import { sortBy } from 'lodash';
import * as React from 'react';
import { Button } from '../../components/controls/buttons';
import Dropdown from '../../components/controls/Dropdown';
import DropdownIcon from '../../components/icons/DropdownIcon';
import { getLocalizedMetricName, translate } from '../../helpers/l10n';
import { isDiffMetric } from '../../helpers/measures';
import { Metric } from '../../types/types';
import AddGraphMetricPopup from './AddGraphMetricPopup';

interface Props {
  metrics: Metric[];
  metricsTypeFilter?: string[];
  onAddMetric: (metric: string) => void;
  onRemoveMetric: (metric: string) => void;
  selectedMetrics: string[];
}

interface State {
  metrics: string[];
  query: string;
  selectedMetrics: string[];
}

export default class AddGraphMetric extends React.PureComponent<Props, State> {
  state: State = {
    metrics: [],
    query: '',
    selectedMetrics: [],
  };

  filterSelected = (query: string, selectedElements: string[]) => {
    return selectedElements.filter((element) =>
      this.getLocalizedMetricNameFromKey(element).toLowerCase().includes(query.toLowerCase())
    );
  };

  filterMetricsElements = (
    { metricsTypeFilter, metrics, selectedMetrics }: Props,
    query: string
  ) => {
    return metrics
      .filter((metric) => {
        if (
          metric.hidden ||
          isDiffMetric(metric.key) ||
          ['DATA', 'DISTRIB'].includes(metric.type) ||
          selectedMetrics.includes(metric.key) ||
          !getLocalizedMetricName(metric).toLowerCase().includes(query.toLowerCase())
        ) {
          return false;
        }
        if (metricsTypeFilter && metricsTypeFilter.length > 0) {
          return metricsTypeFilter.includes(metric.type);
        }
        return true;
      })
      .map((metric) => metric.key);
  };

  getSelectedMetricsElements = (metrics: Metric[], selectedMetrics: string[]) => {
    return metrics
      .filter((metric) => selectedMetrics.includes(metric.key))
      .map((metric) => metric.key);
  };

  getLocalizedMetricNameFromKey = (key: string) => {
    const metric = this.props.metrics.find((m) => m.key === key);
    return metric === undefined ? key : getLocalizedMetricName(metric);
  };

  onSearch = (query: string) => {
    this.setState({ query });
    return Promise.resolve();
  };

  onSelect = (metric: string) => {
    this.props.onAddMetric(metric);
    this.setState((state) => {
      return {
        selectedMetrics: sortBy([...state.selectedMetrics, metric]),
        metrics: this.filterMetricsElements(this.props, state.query),
      };
    });
  };

  onUnselect = (metric: string) => {
    this.props.onRemoveMetric(metric);
    this.setState((state) => {
      return {
        metrics: sortBy([...state.metrics, metric]),
        selectedMetrics: state.selectedMetrics.filter((selected) => selected !== metric),
      };
    });
  };

  render() {
    const { query } = this.state;
    const filteredMetrics = this.filterMetricsElements(this.props, query);
    const selectedMetrics = this.getSelectedMetricsElements(
      this.props.metrics,
      this.props.selectedMetrics
    );
    return (
      <Dropdown
        className="display-inline-block"
        closeOnClick={false}
        closeOnClickOutside={true}
        overlay={
          <AddGraphMetricPopup
            elements={filteredMetrics}
            filterSelected={this.filterSelected}
            metricsTypeFilter={this.props.metricsTypeFilter}
            onSearch={this.onSearch}
            onSelect={this.onSelect}
            onUnselect={this.onUnselect}
            renderLabel={(element) => this.getLocalizedMetricNameFromKey(element)}
            selectedElements={selectedMetrics}
          />
        }
      >
        <Button className="spacer-left">
          <span className="text-ellipsis text-middle">
            {translate('project_activity.graphs.custom.add')}
          </span>
          <DropdownIcon className="text-top little-spacer-left" />
        </Button>
      </Dropdown>
    );
  }
}
