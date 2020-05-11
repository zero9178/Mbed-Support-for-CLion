# Mbed Support for CLion

## Content
1. [Prerequisites](#prerequisites)
2. [Features](#features)
    1. [Project Creation](#project-creation)
    2. [Project Import](#project-import)
    3. [Project Configuration](#project-configuration)
    4. [Changing Target](#changing-target)
    5. [Build Modes](#build-modes)
    6. [Package Management](#package-management)
3. [Tips for Development](#tips-for-development)
4. [Known Issues/TODOs](#known-issuestodos)

## Prerequisites

For the plugin to work, the mbed cli needs to present in the PATH or be explicitly specified in the settings under 
`Tools -> Mbed`. For importing existing repositories from version control, git or mercurial are required.

Installers for Windows and Mac OS can be found at https://os.mbed.com/docs/mbed-os/v5.15/tools/installation-and-setup.html
Recommended method of installation is using python's package manager pip, and the command `pip install mbed-cli`

## Features
### Project Creation

New Mbed projects can be created using CLions new project dialog. Simply select Mbed and enter a valid location in the 
right.

<NEW_PROJECT_DIALOG_SCREENSHOT>

After pressing the create button a new window will open and the plugin starts downloading the newest Mbed OS version 
from the Github repo. As soon as the download has finished a new dialog will appear asking which Mbed Target should be 
used. This option can be changed later at any time. 

<TARGET_DIALOG_SCREENSHOT>

After pressing okay a cmake project will be generated from the Mbed project and CLion should start indexing and 
updating symbols. To further configure your project the file mbed_app.json has been created. To start programing
the file main.cpp has been created that already contains the main function of the program. 

### Project Import

To fetch existing Mbed projects from Git or Mercurial the `mbed import` functionality has been integrated into CLion.
In the `Get from Version Control` dialog simply select `mbed import` as Version Control and enter the URL to the 
repository. 

<GET_FROM_VERSION_CONTROL_SCREENSHOT>

For importing examples provided by Mbed OS the repository name can be specified instead of the URL.

<GET_MBED_OS_EXAMPLE_LORAWAN_SCREENSHOT>

For additional options such as branch, tag or commit hash see:
https://os.mbed.com/docs/mbed-os/v5.15/tools/working-with-mbed-cli.html

After pressing `Clone` the git or Mercurial repository will be downloaded. Once the download has finished the target 
dialog as seen in [Project Creation](#project-creation) will appear.

### Project Configuration

To configure targets and features of Mbed OS the file `mbed_app.json` exists. The CMakeLists.txt that is generated when
creating a new project or importing existing ones is created by the mbed-cli after reading the mbed_app.json file. As the 
CMakeLists.txt file is overwritten each time the `mbed_app.json` file is changed, no changes should be done to it except
adding new Sources. For the documentation on features and possible configurations see: 
https://os.mbed.com/docs/mbed-os/v5.15/reference/configuration.html

The plugin ships with a schema that provides Code Completion, documentation and checks for validity
of the `mbed_app.json` file.

<CODE_COMPLETION_AND_DOC_SCREENSHOT>

After changes have been done the file a prompt will appear at the top, similar to the reload-prompt inside of 
CMakeLists.txt for normal C++ projects, and prompt to reload the Mbed project. A new CMakeLists.txt file is then created 
using the configs in the `mbed_app.json` file.

<RELOAD_PROMPT_SCREENSHOT>

### Changing Target

The Mbed target can be changed at any time in the project. Either right click the project root and select 
`Change Target Board` or go to `Build -> Change Target Board`.

<CHANGE_TARGET_BOARD_CONTEXT_MENU>

### Build Modes

By default, code is compiled in debug mode with optimizations turned off. To switch to release mode and optimize
for size go to `Build -> Switch to Release`. A dialog regenerating the cmake project should appear. Use the menu
`Build -> Switch to Debug` to switch back to unoptimized code.

<SWITCH_TO_RELEASE_SCREENSHOT>

### Package Management

For managing Mbed OS packages a tool window labeled "Mbed" exists at the bottom of the screen. It provides a tree view
of all dependencies of the project and allows changing the version of dependencies and sub dependencies. 

<DEPENDENCY_TREE_SCREENSHOT>

<CHANGING_VERSION_SCREENSHOT>

## Tips for Development

* Use Ninja instead of make: https://github.com/ninja-build/ninja Simply download the binary for your platform,
 add its location in the PATH and specify `-GNinja` in the CMake options in the Settings under 
 `Build, Execution, Deployment -> CMake`. Using Ninja instead of make leads to faster compile times for both incremental
 and full builds.
* Enable Repository caching: Simply enter `mbed cache on` in a terminal to enable Repository caching. The mbed-cli will 
  then cache downloads of repositories and reuse them when possible. This allows for faster project creation as well as 
  creating new projects without an internet connection. 
  
## Known Issues/TODOs
* When working on Windows, and the base path of the project has too many characters the project may not compile.
  This is due to the CMakeLists.txt file generated by the mbed-cli adding every folder in the project to the include-path,
  leading to a compiler command exceeding Windows maximum number of 32768 characters. 
* Ability to remove and add packages will be added in the future
* A prompt for the user to turn on Repository caching after project creation if it is turned off will be added in the future
* Ability to check features of a specific target (such as if it has a lower power clock, etc.) will be added in the future. 