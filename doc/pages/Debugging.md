If something is not working properly or if you want to see what exactly the MCA Selector is doing, debugging can be
enabled in the settings. Informative messages and errors will be printed to the console as well as to a log file.
The log file is stored in the following directory and is overwritten after each new start of the program.

## Windows

`%LOCALAPPDATA%\mcaselector\debug.log` if `%LOCALAPPDATA%` is set, otherwise `<parent directory of mcaselector.jar>\mcaselector\debug.log`

## MacOS

`~/Library/Logs/mcaselector/debug.log`

## Linux

`$XDG_DATA_HOME/mcaselector/debug.log` if `$XDG_DATA_HOME` is set or the first directory that contains a
  file `mcaselector/debug.log` in `$XDG_DATA_DIRS` or `mcaselector` in the first entry in `$XDG_DATA_DIRS` or if
  neither `$XDG_DATA_HOME` nor `$XDG_DATA_DIRS` is set `~/.local/share/mcaselector/debug.log`.

---

Alternatively, the location of the log file can be viewed by clicking on the link in the settings dialog.
