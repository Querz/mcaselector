[Setup]
AppId={{C6145D1F-C820-492A-A649-F4D4C063EECB}
AppName=${applicationName}
AppVersion=${applicationVersion}
;AppVerName=${applicationName} ${applicationVersion}
AppPublisher=${applicationAuthor}
AppPublisherURL=${applicationUrl}
AppSupportURL=${applicationUrl}
AppUpdatesURL=${applicationUrl}
AppCopyright=${applicationCopyright}
DefaultDirName={autopf}\\${applicationName}
DisableProgramGroupPage=yes
LicenseFile=LICENSE
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
OutputBaseFilename=MCA_Selector_Setup
SetupIconFile=icon.ico
Compression=lzma
SolidCompression=yes
WizardStyle=modern
WizardSmallImageFile=small.bmp
WizardImageFile=large.bmp
VersionInfoVersion=${applicationVersion}.0

[Languages]
Name: "en_GB"; MessagesFile: "compiler:Default.isl"
Name: "pt_BR"; MessagesFile: "compiler:Languages\\BrazilianPortuguese.isl"
Name: "cs_CZ"; MessagesFile: "compiler:Languages\\Czech.isl"
Name: "nl_NL"; MessagesFile: "compiler:Languages\\Dutch.isl"
Name: "fr_FR"; MessagesFile: "compiler:Languages\\French.isl"
Name: "de_DE"; MessagesFile: "compiler:Languages\\German.isl"
Name: "it_IT"; MessagesFile: "compiler:Languages\\Italian.isl"
Name: "pl_PL"; MessagesFile: "compiler:Languages\\Polish.isl"
Name: "pt_PT"; MessagesFile: "compiler:Languages\\Portuguese.isl"
Name: "ru_RU"; MessagesFile: "compiler:Languages\\Russian.isl"
Name: "es_ES"; MessagesFile: "compiler:Languages\\Spanish.isl"
Name: "hu_HU"; MessagesFile: "compiler:Languages\\Hungarian.isl"
Name: "ja_JP"; MessagesFile: "compiler:Languages\\Japanese.isl"
Name: "tr_TR"; MessagesFile: "compiler:Languages\\Turkish.isl"
Name: "ko_KR"; MessagesFile: "compiler:Languages\\Korean.isl"
Name: "sv_SE"; MessagesFile: "compiler:Languages\\Swedish.isl"
Name: "uk_UA"; MessagesFile: "compiler:Languages\\Ukrainian.isl"
Name: "zh_CN"; MessagesFile: "Languages\\Unofficial\\ChineseSimplified.isl"
Name: "zh_TW"; MessagesFile: "Languages\\Unofficial\\ChineseTraditional.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "${applicationName}\\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs

[InstallDelete]
Type: filesandordirs; Name: "{app}\\jre"
Type: filesandordirs; Name: "{app}\\lib"
Type: filesandordirs; Name: "{app}\\runtime"
Type: files; Name: "{app}\\*.jar"

[UninstallDelete]
Type: dirifempty; Name: "{app}"

[Icons]
Name: "{autoprograms}\\${applicationName}"; Filename: "{app}\\${applicationName}.exe"
Name: "{autodesktop}\\${applicationName}"; Filename: "{app}\\${applicationName}.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\\${applicationName}.exe"; Description: "{cm:LaunchProgram,${applicationName}}"; Flags: nowait postinstall skipifsilent

[Registry]
Root: HKA; Subkey: "Software\${applicationName}"; ValueType: string; ValueName: "Language"; ValueData: "{language}"; Flags: uninsdeletekey