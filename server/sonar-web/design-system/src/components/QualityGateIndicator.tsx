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
import { useTheme } from '@emotion/react';
import React from 'react';
import { theme as twTheme } from 'twin.macro';
import { BasePlacement, PopupPlacement } from '../helpers/positioning';
import { themeColor, themeContrast } from '../helpers/theme';

const SIZE = {
  sm: twTheme('spacing.4'),
  md: twTheme('spacing.6'),
  xl: twTheme('spacing.16'),
};

type QGStatus = 'ERROR' | 'OK' | 'NONE' | 'NOT_COMPUTED';

interface Props {
  ariaLabel?: string;
  className?: string;
  size?: keyof typeof SIZE;
  status: QGStatus;
  tooltipPlacement?: BasePlacement;
  withTooltip?: boolean;
}

const RX_4 = 4;
const RX_2 = 2;

export default function QualityGateIndicator(props: Props) {
  const {
    className,
    size = 'md',
    status,
    tooltipPlacement = PopupPlacement.Right,
    withTooltip,
    ariaLabel,
  } = props;
  const iconProps = {
    className,
    height: SIZE[size],
    rx: size === 'xl' ? RX_4 : RX_2,
    size,
    tooltipPlacement,
    width: SIZE[size],
    withTooltip,
  };
  let StatusComponent: React.ReactNode;
  switch (status) {
    case 'NONE':
    case 'NOT_COMPUTED':
      StatusComponent = <QGNotComputed {...iconProps} />;
      break;
    case 'OK':
      StatusComponent = <QGPassed {...iconProps} />;
      break;
    case 'ERROR':
      StatusComponent = <QGFailed {...iconProps} />;
      break;
  }
  return (
    <div aria-label={ariaLabel} className="sw-flex sw-justify-center sw-items-center">
      {StatusComponent}
    </div>
  );
}

const COMMON_PROPS = {
  fill: 'none',
  role: 'status',
  xmlns: 'http://www.w3.org/2000/svg',
};

interface IconProps {
  className?: string;
  height: string;
  rx: number;
  size: keyof typeof SIZE;
  tooltipPlacement?: BasePlacement;
  width: string;
  withTooltip?: boolean;
}

function QGNotComputed({
  className,
  rx,
  size,
  tooltipPlacement,
  withTooltip,
  ...sizeProps
}: IconProps) {
  const theme = useTheme();
  const contrastColor = themeContrast('qgIndicatorNotComputed')({ theme });
  return (
    <svg className={className} {...COMMON_PROPS} {...sizeProps}>
      <rect fill={themeColor('qgIndicatorNotComputed')({ theme })} rx={rx} {...sizeProps} />
      {
        {
          xl: <path d="M42 31v3H22v-3z" fill={contrastColor} />,
          md: <path d="M18 12v1.5H6V12z" fill={contrastColor} />,
          sm: <path d="M12 8v1H4V8z" fill={contrastColor} />,
        }[size]
      }
    </svg>
  );
}

function QGPassed({ className, rx, size, tooltipPlacement, withTooltip, ...sizeProps }: IconProps) {
  const theme = useTheme();
  const contrastColor = themeContrast('qgIndicatorPassed')({ theme });
  return (
    <svg className={className} {...COMMON_PROPS} {...sizeProps}>
      <rect fill={themeColor('qgIndicatorPassed')({ theme })} rx={rx} {...sizeProps} />
      {
        {
          xl: (
            <>
              <path d="M38.974 25 41 27.026 28.847 39.178l-2.025-2.025z" fill={contrastColor} />
              <path d="M30.974 37.153 28.95 39.18 22 32.229l2.026-2.025z" fill={contrastColor} />
            </>
          ),
          md: (
            <>
              <path d="m16.95 7.5 1.308 1.307-7.84 7.84-1.308-1.306z" fill={contrastColor} />
              <path d="m11.79 15.34-1.307 1.307-4.484-4.483 1.307-1.306z" fill={contrastColor} />
            </>
          ),
          sm: (
            <>
              <path d="m11.3 5 .871.87-5.227 5.228-.87-.871z" fill={contrastColor} />
              <path d="m7.86 10.227-.872.871L4 8.11l.871-.871z" fill={contrastColor} />
            </>
          ),
        }[size]
      }
    </svg>
  );
}

function QGFailed({ className, rx, size, tooltipPlacement, withTooltip, ...sizeProps }: IconProps) {
  const theme = useTheme();
  const contrastColor = themeContrast('qgIndicatorFailed')({ theme });
  return (
    <svg className={className} {...COMMON_PROPS} {...sizeProps}>
      <rect fill={themeColor('qgIndicatorFailed')({ theme })} rx={rx} {...sizeProps} />
      {
        {
          xl: (
            <>
              <path d="m37.153 25 2.026 2.026-12.153 12.152L25 37.153z" fill={contrastColor} />
              <path d="m39.178 37.153-2.025 2.026L25 27.026 27.026 25z" fill={contrastColor} />
            </>
          ),
          md: (
            <>
              <path d="m15.34 7.5 1.307 1.307-7.84 7.84L7.5 15.34z" fill={contrastColor} />
              <path d="m16.647 15.34-1.307 1.307-7.84-7.84L8.806 7.5z" fill={contrastColor} />
            </>
          ),
          sm: (
            <>
              <path d="m10.227 5 .871.871-5.227 5.227L5 10.227z" fill={contrastColor} />
              <path d="m11.098 10.227-.871.87L5 5.872 5.87 5z" fill={contrastColor} />
            </>
          ),
        }[size]
      }
    </svg>
  );
}
