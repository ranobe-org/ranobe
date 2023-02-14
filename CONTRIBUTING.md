## General

- You (contributor) are allowed to update any piece of code you see fit.
- Few guidelines
    - `Constants` should be defined in `config/Ranobe.java`
    - Follow clean coding standards
    - Try not to add third-party libraries (for package sizes concerns)
    - If possible, minimize the app size (that would be great)

## Adding a new source

- The file for the source should be added under `/sources/<lang>/`
- The name of the file should be `<SiteName>.java`
- Implement `Source.java` interface for adding a new source
- If for some reason a method is not possible to implement throw a proper `Exception` with the
  message
    - Ex ```throw new Exception("I am sorry")```
- Test the source properly before creating a PR
- Once done, raise a PR with the title `Add new source: <SOURCE_NAME_HERE>`, the branch name can
  be `source/<source_name>`

## Rules

- Do not define `Novel` and `NovelItem`'s `id` field since it is calculated in the constructor.
- Do not define `Chapter` and `ChapterItem`'s `novelId` field since it is calculated in the
  constructor,
  whereas the `id` field should be the chapter no for the novel
