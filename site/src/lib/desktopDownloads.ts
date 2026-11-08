export type OsId = 'windows' | 'macos' | 'linux';
export type ArchId = 'x64' | 'arm64';

export interface DesktopAsset {
  name: string;
  url: string;
  size: number;
}

export interface DesktopManifest {
  tag: string;
  version: string;
  publishedAt: string;
  prerelease: boolean;
  assets: DesktopAsset[];
}

export type PlatformKey =
  | 'windows-x64'
  | 'windows-arm64'
  | 'macos-x64'
  | 'macos-arm64'
  | 'linux-x64'
  | 'linux-arm64';

/** Primary installer patterns per platform (order = preference). */
const PRIMARY: Record<PlatformKey, RegExp[]> = {
  'windows-x64': [/-win-x64-nsis\.exe$/i],
  'windows-arm64': [/-win-arm64-nsis\.exe$/i],
  'macos-x64': [/-mac-x64\.dmg$/i],
  'macos-arm64': [/-mac-arm64\.dmg$/i],
  'linux-x64': [/-linux-x86_64\.AppImage$/i, /-linux-amd64\.AppImage$/i],
  'linux-arm64': [/-linux-arm64\.AppImage$/i, /-linux-aarch64\.AppImage$/i],
};

const PLATFORM_ASSET: Record<PlatformKey, RegExp> = {
  'windows-x64': /-win-x64/i,
  'windows-arm64': /-win-arm64/i,
  'macos-x64': /-mac-x64/i,
  'macos-arm64': /-mac-arm64/i,
  'linux-x64': /-linux-(?:x86_64|amd64|x64)(?:\.|-)/i,
  'linux-arm64': /-linux-(?:arm64|aarch64)(?:\.|-)/i,
};

export const PLATFORM_LABELS: Record<PlatformKey, string> = {
  'windows-x64': 'Windows (x64)',
  'windows-arm64': 'Windows (arm64)',
  'macos-x64': 'macOS (Intel)',
  'macos-arm64': 'macOS (Apple Silicon)',
  'linux-x64': 'Linux (x64)',
  'linux-arm64': 'Linux (arm64)',
};

export const ALL_PLATFORMS: PlatformKey[] = [
  'windows-x64',
  'windows-arm64',
  'macos-arm64',
  'macos-x64',
  'linux-x64',
  'linux-arm64',
];

export function platformKey(os: OsId, arch: ArchId): PlatformKey {
  return `${os}-${arch}` as PlatformKey;
}

export function findPrimary(
  assets: DesktopAsset[],
  key: PlatformKey,
): DesktopAsset | undefined {
  for (const re of PRIMARY[key]) {
    const hit = assets.find((a) => re.test(a.name));
    if (hit) return hit;
  }
  return undefined;
}

export function findAlternatives(
  assets: DesktopAsset[],
  key: PlatformKey,
  primary?: DesktopAsset,
): DesktopAsset[] {
  const re = PLATFORM_ASSET[key];
  return assets.filter(
    (a) => re.test(a.name) && a.name !== primary?.name,
  );
}

export function formatBytes(size: number): string {
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(0)} KB`;
  return `${(size / (1024 * 1024)).toFixed(0)} MB`;
}

export function assetFormatLabel(name: string): string {
  if (/-nsis\.exe$/i.test(name)) return 'Installer (.exe)';
  if (/-portable\.exe$/i.test(name)) return 'Portable (.exe)';
  if (/\.msi$/i.test(name)) return '.msi';
  if (/\.dmg$/i.test(name)) return '.dmg';
  if (/\.zip$/i.test(name)) return '.zip';
  if (/\.AppImage$/i.test(name)) return '.AppImage';
  if (/\.deb$/i.test(name)) return '.deb';
  if (/\.rpm$/i.test(name)) return '.rpm';
  if (/\.snap$/i.test(name)) return '.snap';
  if (/\.pacman$/i.test(name)) return '.pacman';
  return name;
}

export const OS_LIST: OsId[] = ['windows', 'macos', 'linux'];

export const OS_LABEL: Record<OsId, string> = {
  windows: 'Windows',
  macos: 'macOS',
  linux: 'Linux',
};

export const ARCH_LIST: ArchId[] = ['x64', 'arm64'];

export const ARCH_LABEL: Record<ArchId, string> = {
  x64: 'x64',
  arm64: 'ARM64',
};

export function isOsId(value: string | null | undefined): value is OsId {
  return value === 'windows' || value === 'macos' || value === 'linux';
}

export function formatPublishedAt(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  return date.toLocaleDateString('en-GB', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });
}
