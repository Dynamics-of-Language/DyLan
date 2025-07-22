# babyDS-backup

This is a branch for an emergency back up of all things babyDS (NOT CLEAN AT ALL, BUT I HAD NO CHOICE). It's here because I made a few local copies of DS while working on the BabyDS branch; now I've learned my lesson.

There are a bunch of strange/new folders here and I will describe these over time...

- `lib` and `util` seems to be copies of dylan_util that Cursor made when loading the whole code and trying to fix all the local project paths... Also made lots of modifications to relevant files, such as pom.xml...

The good things about this branch:

- It works in Cursor (and therefore vscode?)
- I finally have a backup after all the untidy local changes
- dylan_util most probably is not being used anymore, as the packages are all copied over (by mistake, a good one though).

The bad things:

- It is very untidy and doesn't properly follow what we had in the previous branches like arash_dsttr or babyDS.

Anyway, I think DS code clean up should happen at all levels, not just this.
