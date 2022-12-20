<p align="center">
  <img width="300" src="https://github.com/ranobe-org/ranobe/raw/main/assets/app.gif" alt="app gif">
</p>
<div align="center">
    <h2><samp>R.A.N.O.B.E</samp></h2>
    <samp>A simple extensible light novel reader</samp>
    <br/><br/>
    <a href="https://github.com/ranobe-org/ranobe/releases/download/v0.0.1/ranobe.apk" title="Download">
        <img height='30' src="https://img.shields.io/badge/download-2da44e?style=flat&logo=android&logoColor=white" alt="Download" title="Download">
    </a>
    <a href="https://ranobe-org.github.io/.github/" title="Website">
        <img height='30' src="https://img.shields.io/badge/website-F4F51E?style=flat&logo=internet-archive&logoColor=black" alt="Website" title="Website">
    </a>
    <a href="https://discord.gg/96wsWZ6M" title="Join Discord">
        <img height='30' src="https://img.shields.io/badge/discord-5865F2?style=flat&logo=discord&logoColor=white" alt="Join Discord" title="Join Discord">
    </a>
    <a href="https://github.com/ranobe-org" title="Github">
        <img height='30' src="https://img.shields.io/badge/github-ffffff?style=flat&logo=github&logoColor=black" alt="Check Github" title="Check Github">
    </a>
</div>

---------------

### Features
- Read Novels
- No Ads, No Paywall
- Unlimited novel browsing
- Multiple Sources: LightNovelBtt, ReadLightNove, VipNovels
- Searching with keywords
- Chapter Search and Sort
- Continuous Vertical Reading
- Light and Dark modes
- Customize reading experience
- Change font size
- Change background and font color

----------------

### TODO

#### must have

- [x] basic source browsing and reading
- [x] improve ui for details and chapters page
- [x] chapter sort
- [x] cache http responses (reduce load on sources)
- [x] chapter search
- [x] complete search page with keyword search
- [x] reader text formatting with font size and bg/text color selection
- [x] continuous vertical scroll for reader
- [x] add error text/art to denote no results found in search
- [x] update app when a new source is selected
- [x] add settings page with theme selection
- [x] sources list selection
- [x] remembering source selection (only one source at a time, that's the rule)
- [ ] database
    - [ ] add to library, only store novel in db
    - [ ] download chapters for offline view, save everything to db
    - [ ] saving read state for each item
- [ ] more sources (currently has 3)

#### could have

- [ ] advanced search techniques
- [ ] source management tools ?
- [ ] export into x formats
- [ ] metrics if possible
- [ ] backup data, migrations
- [ ] novel updates

---------------

### Guidelines

1. Sources should not define `Novel` and `NovelItem`'s `id` field since it is calculated in constructor.
2. Sources should not define `Chapter` and `ChapterItem`'s `novelId` field since it is calculated in constructor,
whereas the `id` field should be the chapter no for the novel
