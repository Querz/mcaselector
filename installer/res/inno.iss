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
ExtraDiskSpaceRequired=223010816
VersionInfoVersion=${applicationVersion}.0

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "brazilianportuguese"; MessagesFile: "compiler:Languages\\BrazilianPortuguese.isl"
Name: "czech"; MessagesFile: "compiler:Languages\\Czech.isl"
Name: "dutch"; MessagesFile: "compiler:Languages\\Dutch.isl"
Name: "french"; MessagesFile: "compiler:Languages\\French.isl"
Name: "german"; MessagesFile: "compiler:Languages\\German.isl"
Name: "italian"; MessagesFile: "compiler:Languages\\Italian.isl"
Name: "polish"; MessagesFile: "compiler:Languages\\Polish.isl"
Name: "portuguese"; MessagesFile: "compiler:Languages\\Portuguese.isl"
Name: "russian"; MessagesFile: "compiler:Languages\\Russian.isl"
Name: "spanish"; MessagesFile: "compiler:Languages\\Spanish.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "${applicationName}.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "7za.exe"; DestDir: "{tmp}"; Flags: deleteafterinstall
Source: "${applicationJar}"; DestDir: "{app}"; Flags: ignoreversion
Source: "lib\\*"; DestDir: "{app}\\lib"; Flags: ignoreversion

[InstallDelete]
Type: filesandordirs; Name: "{app}\\jre"
Type: filesandordirs; Name: "{app}\\lib"

[UninstallDelete]
Type: filesandordirs; Name: "{app}\\jre"
Type: filesandordirs; Name: "{app}\\zulu21.32.17-ca-fx-jre21.0.2-win_x64"
Type: filesandordirs; Name: "{app}\\MCA Selector.ini"

[Icons]
Name: "{autoprograms}\\${applicationName}"; Filename: "{app}\\${applicationName}.exe"
Name: "{autodesktop}\\${applicationName}"; Filename: "{app}\\${applicationName}.exe"; Tasks: desktopicon

[Run]
Filename: "{tmp}\\7za.exe"; Parameters: "x -y {tmp}\\jre.zip"; WorkingDir: "{app}"; AfterInstall: RenameJRE; Flags: runhidden
Filename: "{app}\\${applicationName}.exe"; Description: "{cm:LaunchProgram,${applicationName}}"; Flags: nowait postinstall skipifsilent

[Code]

var DownloadPage: TDownloadWizardPage;

function OnDownloadProgress(const Url, FileName: String; const Progress, ProgressMax: Int64): Boolean;
begin
	if Progress = ProgressMax then
		Log(Format('Successfully downloaded file to {tmp}: %s', [FileName]));
	Result := True;
end;

procedure InitializeWizard;
begin
	DownloadPage := CreateDownloadPage(SetupMessage(msgWizardPreparing), SetupMessage(msgPreparingDesc), @OnDownloadProgress);
end;

function NextButtonClick(CurPageID: Integer): Boolean;
begin
	if CurPageID = wpReady then begin
		DownloadPage.Clear;
		DownloadPage.Add('https://cdn.azul.com/zulu/bin/zulu21.32.17-ca-fx-jre21.0.2-win_x64.zip', 'jre.zip', '');
		DownloadPage.Show;
		try
			try
				DownloadPage.Download;
				Result := True;
			except
				SuppressibleMsgBox(AddPeriod(GetExceptionMessage), mbCriticalError, MB_OK, IDOK);
				Result := False;
			end;
		finally
			DownloadPage.Hide;
		end;
	end else
		Result := True;
end;

procedure RenameJRE;
begin
	Log('Renaming jre directory');
	if not RenameFile(ExpandConstant('{app}\\zulu21.32.17-ca-fx-jre21.0.2-win_x64'), ExpandConstant('{app}\\jre')) then begin
		Log('Failed to rename jre folder, creating custom ini');
		SaveStringToFile(ExpandConstant('{app}\\MCA Selector.ini'), 'vm.location=zulu21.32.17-ca-fx-jre21.0.2-win_x64\\bin\\server\\jvm.dll', False);
	end;
end;