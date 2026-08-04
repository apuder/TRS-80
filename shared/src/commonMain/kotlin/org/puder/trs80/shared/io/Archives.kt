/*
 * Copyright The TRS-80 App Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.puder.trs80.shared.io

import okio.FileSystem
import okio.Path

/**
 * Opens [archive] as a file system of its own, to read entries out of.
 *
 * A ZIP, in practice: it is how the ROMs arrive. okio can do this everywhere it
 * has a real file system to open the archive from, which is everywhere except
 * the browser -- so this is null there, and the caller says so rather than
 * pretending the file was empty.
 */
expect fun FileSystem.openArchive(archive: Path): FileSystem?
