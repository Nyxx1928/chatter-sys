const SlackColors = {
  light: {
    primary: '#4A154B',
    primaryLight: '#7C2382',
    accentGreen: '#2EB67D',
    accentBlue: '#36C5F0',
    accentYellow: '#ECB22E',
    accentRed: '#E01E5A',
    surfacePrimary: '#FFFFFF',
    surfaceSecondary: '#F4EDE4',
    surfaceTertiary: '#E8E0D5',
    textPrimary: '#1D1C1D',
    textSecondary: '#616061',
    textInverse: '#FFFFFF',
    border: '#DDD9D4',
  },
  dark: {
    primary: '#611F69',
    primaryLight: '#7C2382',
    accentGreen: '#2EB67D',
    accentBlue: '#36C5F0',
    accentYellow: '#ECB22E',
    accentRed: '#E01E5A',
    surfacePrimary: '#1A1D21',
    surfaceSecondary: '#222529',
    surfaceTertiary: '#2D2F33',
    textPrimary: '#D1D2D3',
    textSecondary: '#9D9EA0',
    textInverse: '#1D1C1D',
    border: '#424448',
  },
};

export default {
  light: {
    text: SlackColors.light.textPrimary,
    background: SlackColors.light.surfacePrimary,
    tint: SlackColors.light.primary,
    tabIconDefault: SlackColors.light.textSecondary,
    tabIconSelected: SlackColors.light.primary,
    ...SlackColors.light,
  },
  dark: {
    text: SlackColors.dark.textPrimary,
    background: SlackColors.dark.surfacePrimary,
    tint: SlackColors.dark.primary,
    tabIconDefault: SlackColors.dark.textSecondary,
    tabIconSelected: SlackColors.dark.primary,
    ...SlackColors.dark,
  },
};

export { SlackColors };
