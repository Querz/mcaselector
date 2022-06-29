# Download and Installation

**MCA Selector modifies and deletes chunks in your Minecraft world. Please make backups of your world before using.**

---

[**Download Version <!--vs-->2.0.2<!--ve--> (Windows Installer)**](https://github.com/Querz/mcaselector/releases/download/2.0.2/MCA_Selector_Setup.exe)

"Requirements":
* Windows 7-10 64bit
* At least 4 GB of RAM

---

[**Download Version <!--vs-->2.0.2<!--ve--> (Universal)**](https://github.com/Querz/mcaselector/releases/download/2.0.2/mcaselector-2.0.2.jar)

"Requirements":
* Either:
  * 64bit JRE 17+, with JavaFX either pre-packed or installed separately
* A computer
  * At least 6 GB of RAM. If lower, more RAM has to manually be assigned to the JVM using the `-Xmx` argument.
    Assigning 4 GB is recommended.
* A brain

### Using a JRE or JDK with pre-packed JavaFX (the simple way)
The following steps describe a way to run MCA Selector using a JRE or JDK with pre-packed JavaFX.

With zulu JRE-FX:
For Windows:

* Download the JRE-FX-17 from [HERE](https://cdn.azul.com/zulu/bin/zulu17.30.15-ca-fx-jre17.0.1-win_x64.zip) into an
  empty folder.
* Move the previously downloaded `mcaselector-2.0.2.jar` into the same folder. Extract the content of the
  downloaded .zip file into this folder.
* Hold `Shift` and `right-click` an empty spot in this folder, and choose `Open PowerShell window here`. Type `& `
  (with a space at the end), go into the previously extracted folder and drag and drop the `bin\java.exe` file into
  the PowerShell window. Then complete the command by typing a space and ` -jar mcaselector-2.0.2.jar` and press
  `Enter`.

For MacOS:

* Download the JRE-FX-17 from [HERE](https://cdn.azul.com/zulu/bin/zulu17.30.51-ca-fx-jre17.0.1-macosx_x64.zip) ([HERE](https://cdn.azul.com/zulu/bin/zulu17.34.19-ca-fx-jre17.0.3-macosx_aarch64.zip) for Apple M1) into
  an empty folder.
* Extract the content of the downloaded .zip file into this folder.
* Press `Cmd+Space`, type `Terminal` and press `Enter`. Go into the previously extracted folder and drag and drop
  the `zulu-17.jdk/Contents/Home/bin/java` into the terminal. Continue to type `-jar ` (with a space at the end) and
  drag-and drop the `mcaselector-2.0.2.jar` into the terminal as well, the press `Enter`.

For Linux:

* Download the JRE-FX-17 from [HERE](https://cdn.azul.com/zulu/bin/zulu17.30.15-ca-fx-jre17.0.1-linux_x64.tar.gz)
  into an empty folder.
* Extract the content of the downloaded .tar.gz file into this folder.
* Open your terminal of choice and navigate in the extracted folder into `bin`, then run `./java -jar
  <path-to-mcaselector-2.0.2.jar>`.

To avoid having to go through this process every time to start MCA Selector, the resulting command can be copied
into a `.bat`-file on Windows or `.sh`-file on MacOS and Linux and can then be executed by double-clicking the
`.bat`-file on Windows or running `sh <file>.sh` in the terminal / console on MacOS or Linux where `<file>` must be
replaced by the name of the `.sh`-file.

### When you receive an error from a previously installed version of Java
"When I run `mcaselector-2.0.2.jar`, an error dialog appears that looks like this:"

<p align="center">
[[/images/Installation/missing_javafx.png|Popup dialog stating that a JavaFX installation is missing]]
</p>

Open the console or terminal on your OS.

For Windows:
* Hold `Shift` and Right-click on an empty space on your desktop and select `Open PowerShell here` (`Open Command
  window here` on Windows 8 and earlier).

For MacOS:
* Press `Cmd+Space`, type `Terminal` and press `Enter`.

Type the command `java -version` and press `Enter`. If the output shows that your java command is linked to a Java
version older than 17, make sure that you have Java 17 installed or proceed with [Using a JRE or JDK with pre-packed
JavaFX (the simple way)](#using-a-jre-or-jdk-with-pre-packed-javafx-the-simple-way).

For Windows and MacOS:
* Type `java -jar ` (with a space at the end) and drag and drop the `mcaselector-2.0.2.jar` into the console and
  hit `Enter`.

For Linux:
* Run `java -jar <path to mcaselector-2.0.2.jar` where you replace everything in `<>`.

For Windows:
* Download "JavaFX Windows SDK" for your Java version from [here](https://gluonhq.com/products/javafx/).
* Unzip the `.zip`-file with your program of choice, then navigate into the unzipped folder.
* Hold `Shift` and Right-click on an empty space in that folder and select `Open PowerShell window here` (`Open
  Command window here` on Windows 8 and earlier). Type `java --module-path ` (with a space at the end), then drag
  and drop the `lib`-folder into the console. Continue to type ` --add-modules ALL-MODULE-PATH -jar ` (with a space
  at the beginning and the end), then drag and drop the `mcaselector-2.0.2.jar` into the console and hit `Enter`.

For MacOS:
* Download "JavaFX Mac OS X SDK" for your Java version from [here](https://gluonhq.com/products/javafx/).
* Double-click the `.zip`-file to unpack, then navigate into the unzipped folder.
* Press `Cmd+Space`, type `Terminal` and press `Enter`. Type `java --module-path ` (with a space at the end), then
  drag and drop the `lib`-folder into the console. Continue to type `--add-modules ALL-MODULE-PATH -jar ` (with a
  space at the end), then drag and drop the `mcaselector-2.0.2.jar` into the console and hit `Enter`.

For Linux:
* Download "JavaFX Linux SDK" for your Java version from [here](https://gluonhq.com/products/javafx/).
* Unzip the `.zip`-file with your program of choice.
* Open the command prompt and run `java --module-path <path to unzipped folder>/lib --add-modules ALL-MODULE-PATH
  -jar <path to mcaselector-2.0.2.jar>` where you replace everything in `<>` with the appropriate paths.
* Some distributions like AdoptOpenJDK (shipped with most Linux distributions) do not ship with JavaFX by default.
  On Debian, an open version of JavaFX is contained in the `openjfx` package. This or some other installation of
  JavaFX is required to run the `mcaselector-2.0.2.jar`.

To avoid having to go through this process every time to start MCA Selector, the resulting command can be copied
into a `.bat`-file on Windows or `.sh`-file on MacOS and Linux and can then be executed by double-clicking the
`.bat`-file on Windows or running `sh <file>.sh` in the terminal / console on MacOS or Linux where `<file>` must be
replaced by the name of the `.sh`-file.

---

If none of these instructions work, apply "A brain" that you providently held ready after having read the
"Requirements" section carefully. Or ask your question on [Discord](https://discord.gg/h942U8U).