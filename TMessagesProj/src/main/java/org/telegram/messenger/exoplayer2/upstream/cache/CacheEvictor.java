/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baranak.tsupergrap.exoplayer2.upstream.cache;

/**
 * Evicts data from a {@link Cache}. Implementations should call {@link Cache#removeSpan(CacheSpan)}
 * to evict cache entries based on their eviction policies.
 */
public interface CacheEvictor extends Cache.Listener {

  /**
   * Called when cache has been initialized.
   */
  void onCacheInitialized();

  /**
   * Called when a writer starts writing to the cache.
   *
   * @param cache The source of the event.
   * @param key The key being written.
   * @param position The starting position of the data being written.
   * @param maxLength The maximum length of the data being written.
   */
  void onStartFile(Cache cache, String key, long position, long maxLength);

}
