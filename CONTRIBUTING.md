## Translations
English and Chinese Simplified are maintained by the developer. If you find inappropriate wording or missing translations, please open an issue or submit a pull request with details.

For languages other than English and Chinese Simplified, please don't create pull requests for translations; instead, use [Weblate](https://hosted.weblate.org/engage/installerx-revived/).

## Reporting bugs
Before reporting a bug, you should first consult the [FAQ](https://wxxsfxyzm.github.io/InstallerX-Revived/guide/faq.html).

If the issue still exists, [Open an issue](https://github.com/wxxsfxyzm/InstallerX-Revived/issues) and describe the bug including steps to reproduce it.

## Suggesting features
[Open an issue](https://github.com/wxxsfxyzm/InstallerX-Revived/issues) describing the feature you want and your reason for it.

## Building the Project
To build the project locally, you need to set up your environment properly.

### Prerequisites
- **JDK 25**: This project requires JDK 25. Please ensure your `JAVA_HOME` is configured correctly.

### GitHub Packages Authentication
This project uses the snapshot version of the `miuix` library, which is hosted on GitHub Packages. GitHub requires authentication to download packages, even for public projects.

To compile the code, you must configure your **global** `gradle.properties` file (usually located at `~/.gradle/gradle.properties` on Linux/macOS or `%USERPROFILE%\.gradle\gradle.properties` on Windows):

1. Generate a [GitHub Personal Access Token (Classic)](https://github.com/settings/tokens) with the **`read:packages`** scope.
2. Add your GitHub username and the token to the file:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_PERSONAL_ACCESS_TOKEN
```
**Note:** Do not commit these credentials to the project repository. Always use the global Gradle properties file.

## Code
Fork the InstallerX Revived repository, make your changes, and open a pull request.

Please note:
- Describe clearly what your changes do and the problem they solve.
- In the pull request description, specify the device(s) and Android version(s) on which the changes were tested.
- Follow the existing coding style and project conventions.
- Make sure the project builds successfully before submitting the pull request.
- Format your code using Android Studio and optimize imports before committing.
