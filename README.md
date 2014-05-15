LockPic
=======

This projects holds the code for the LockPic application. LockPic is an open source Android application for secure image sharing in social networks.

Specifically, we introduce an Android application which encrypts sensitive regions on images (particularly, faces) in a reversible way leaving the rest of the picture unaltered. This way, any image could be freely published in any social network, and viewed by as many users as the platform allows, while its full content would only be decryptable with a key, maintaining the privacy on the selected areas.

This application is available, free of charge, at Google Play: https://play.google.com/store/apps/details?id=es.uma.lcc.nativejpegencoder

Contents of this project
========================

1. /Patches:
This project uses Sean Barrett's stb_image.c available at http://nothings.org/, and Rich Geldreich's jpeg compressor, available here: https://code.google.com/p/jpeg-compressor/.
Both projects had to be modified for our purposes. The Patches folder contains the original files (subfolder Originals), as well as the .patch files to include our modifications. The patched files are also available at the /jni folder.

2. build-instructions:
These files (pdf with images, and plaintext) contain instructions to build the code (both Java and native sides).

The remaining files and folders are the code (Java side at /src, C side at /jni), libraries, and compiled binaries.

This project is protected by GNU General Public License v3.0.
