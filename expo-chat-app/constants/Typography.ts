import { Platform } from 'react-native';

const fontFamily = Platform.select({
  ios: 'Noto Sans',
  android: 'Noto Sans',
  default: 'Noto Sans',
});

const fontFamilyDisplay = Platform.select({
  ios: 'Noto Sans Display',
  android: 'Noto Sans Display',
  default: 'Noto Sans Display',
});

export const SlackTypography = {
  displayXl: {
    fontFamily: fontFamilyDisplay,
    fontSize: 32,
    fontWeight: '700' as const,
    lineHeight: 38,
  },
  displayLg: {
    fontFamily: fontFamilyDisplay,
    fontSize: 24,
    fontWeight: '700' as const,
    lineHeight: 30,
  },
  displayMd: {
    fontFamily: fontFamilyDisplay,
    fontSize: 20,
    fontWeight: '600' as const,
    lineHeight: 26,
  },
  bodyLg: {
    fontFamily,
    fontSize: 16,
    fontWeight: '400' as const,
    lineHeight: 24,
  },
  bodyMd: {
    fontFamily,
    fontSize: 15,
    fontWeight: '400' as const,
    lineHeight: 22,
  },
  bodySm: {
    fontFamily,
    fontSize: 13,
    fontWeight: '400' as const,
    lineHeight: 18,
  },
  caption: {
    fontFamily,
    fontSize: 11,
    fontWeight: '400' as const,
    lineHeight: 14,
  },
};

export default SlackTypography;
