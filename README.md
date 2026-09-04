# Chooser

## Why this fork exists

Forked from [upstream](https://github.com/cyph/cordova-plugin-chooser) because we needed a different `getFile()` contract: files are returned as a path + metadata (name, mimeType, extension, size) under an enforced `maxFileSize` limit, instead of being loaded whole into memory as base64 data.

Published as [`@herdwatch/cordova-plugin-chooser`](https://www.npmjs.com/package/@herdwatch/cordova-plugin-chooser).

Changes from upstream:
- Replaced the `accept` string + `includeData` boolean arguments with a single `options` object (`mimeTypes`, `maxFileSize`), and removed the separate `getFileMetadata()` method.
- Added a `maxFileSize` check on Android and iOS that rejects the pick with an "Invalid size" error instead of returning oversized files.
- Changed the return payload from in-memory base64 `data`/`dataURI` to a `path` on disk plus `name`, `displayName`, `mimeType`, `extension`, and `size`.
- Dropped the `cordova-plugin-add-swift-support` dependency from `plugin.xml`.
- Declared `supportedInterfaceOrientations` on the iOS picker so it adopts the app's orientations instead of UIKit's default mask, which could be disjoint from them and crash the app on presentation.

## Demo 
[cordova-plugin-chooser-lab](https://github.com/MaximBelov/cordova-plugin-chooser-lab)

## Overview

File chooser plugin for Cordova.

Install with Cordova CLI:

	$ cordova plugin add @herdwatch/cordova-plugin-chooser

Supported Platforms:

* Android

* iOS

## API

	/**
	 * Displays native prompt for user to select a file.
	 *
	 * @param {Object} options
	 * @param {string} options.mimeTypes Optional MIME type filter (e.g. 'image/gif,video/*')
	 * @param {number} options.maxFileSize 
	 *
	 * @returns Promise containing selected file's 
	 * path, display name, MIME type, , and original URI.
	 *
	 * If user cancels, promise will be resolved as undefined.
	 * If error occurs, promise will be rejected.
	 */
	chooser.getFile(options?: {}) : Promise<undefined|{
		path: string;
		name: string;
		mimeType: string;
		extension: string;
		size: number;
	}>

## Example Usage

	(async () => {
		const file = await chooser.getFile();
		console.log(file ? file.name : 'canceled');
	})();


## Platform-Specific Notes

The following must be added to config.xml to prevent crashing when selecting large files
on Android:

```
<platform name="android">
	<edit-config
		file="app/src/main/AndroidManifest.xml"
		mode="merge"
		target="/manifest/application"
	>
		<application android:largeHeap="true" />
	</edit-config>
</platform>
```

If it isn't present already, you'll also need the attribute `xmlns:android="http://schemas.android.com/apk/res/android"` added to your `<widget>` tag in order for that to build successfully.
