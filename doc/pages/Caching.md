MCA Selector creates an image for each region from the provided mca-files. These images are saved separately inside the respective operating system's specific cache folders. Experience showed that a Minecraft world with a size of 10GB resulted in cached image files with a total size of roughly 50MB. Caching as many regions as possible significantly improves loading times.

## Windows

`%LOCALAPPDATA%\mcaselector\cache` if `%LOCALAPPDATA%` is set, otherwise `<parent directory of mcaselector.jar>\mcaselector\cache`

## MacOS

`~/Library/Caches/mcaselector`

## Linux

`$XDG_CACHE_HOME/mcaselector` if `$XDG_CACHE_HOME` is set or the first directory that contains a folder `mcaselector` in `$XDG_CACHE_DIRS` or a new directory called `mcaselector` in the first entry in `$XDG_CACHE_DIRS` is created or if neither `$XDG_CACHE_HOME` nor `$XDG_CACHE_DIRS` is set `~/.cache/mcaselector`.
