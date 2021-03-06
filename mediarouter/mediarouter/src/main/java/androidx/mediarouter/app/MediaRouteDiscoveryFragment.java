/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.mediarouter.app;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;

/**
 * Media route discovery fragment.
 * <p>
 * This fragment takes care of registering a callback with proper flags for media route discovery:
 * <ul>
 *      <li>In {@link Fragment#onCreate} phase, the callback is registered with zero flags. </li>
 *      <li>In {@link Fragment#onStart} phase, the callback's flags are set to the value
 *          which is provided by {@link #onPrepareCallbackFlags()}.</li>
 *      <li>In {@link Fragment#onStop()} phase, the callback's flags are set to zero. </li>
 * </ul>
 * </p><p>
 * The application must supply a route selector to specify the kinds of routes
 * to discover.  The application may also override {@link #onCreateCallback} to
 * provide the {@link MediaRouter} callback to register.
 * </p><p>
 * Note that the discovery callback makes the application be connected with all the
 * {@link androidx.mediarouter.media.MediaRouteProviderService media route provider services}
 * while it is registered.
 * </p>
 */
public class MediaRouteDiscoveryFragment extends Fragment {
    private static final String ARGUMENT_SELECTOR = "selector";

    private MediaRouter mRouter;
    private MediaRouteSelector mSelector;
    private MediaRouter.Callback mCallback;

    public MediaRouteDiscoveryFragment() {
    }

    /**
     * Gets the media router instance.
     */
    public MediaRouter getMediaRouter() {
        ensureRouter();
        return mRouter;
    }

    private void ensureRouter() {
        if (mRouter == null) {
            mRouter = MediaRouter.getInstance(getContext());
        }
    }

    /**
     * Gets the media route selector for filtering the routes to be discovered.
     *
     * @return The selector, never null.
     */
    public MediaRouteSelector getRouteSelector() {
        ensureRouteSelector();
        return mSelector;
    }

    /**
     * Sets the media route selector for filtering the routes to be discovered.
     * This method must be called before the fragment is added.
     *
     * @param selector The selector to set.
     */
    public void setRouteSelector(MediaRouteSelector selector) {
        if (selector == null) {
            throw new IllegalArgumentException("selector must not be null");
        }

        ensureRouteSelector();
        if (!mSelector.equals(selector)) {
            mSelector = selector;

            Bundle args = getArguments();
            if (args == null) {
                args = new Bundle();
            }
            args.putBundle(ARGUMENT_SELECTOR, selector.asBundle());
            setArguments(args);

            if (mCallback != null) {
                mRouter.removeCallback(mCallback);
                mRouter.addCallback(mSelector, mCallback, onPrepareCallbackFlags());
            }
        }
    }

    private void ensureRouteSelector() {
        if (mSelector == null) {
            Bundle args = getArguments();
            if (args != null) {
                mSelector = MediaRouteSelector.fromBundle(args.getBundle(ARGUMENT_SELECTOR));
            }
            if (mSelector == null) {
                mSelector = MediaRouteSelector.EMPTY;
            }
        }
    }

    /**
     * Called to create the {@link androidx.mediarouter.media.MediaRouter.Callback callback}
     * that will be registered.
     * <p>
     * The default callback does nothing.  The application may override this method to
     * supply its own callback.
     * </p>
     *
     * @return The new callback, or null if no callback should be registered.
     */
    public MediaRouter.Callback onCreateCallback() {
        return new MediaRouter.Callback() { };
    }

    /**
     * Called to prepare the callback flags that will be used when the
     * {@link androidx.mediarouter.media.MediaRouter.Callback callback} is registered.
     * <p>
     * The default implementation returns {@link MediaRouter#CALLBACK_FLAG_REQUEST_DISCOVERY}.
     * </p>
     *
     * @return The desired callback flags.
     */
    public int onPrepareCallbackFlags() {
        return MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ensureRouteSelector();
        ensureRouter();
        mCallback = onCreateCallback();
        if (mCallback != null) {
            mRouter.addCallback(mSelector, mCallback, 0);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mCallback != null) {
            mRouter.addCallback(mSelector, mCallback, onPrepareCallbackFlags());
        }
    }

    @Override
    public void onStop() {
        if (mCallback != null) {
            mRouter.addCallback(mSelector, mCallback, 0);
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (mCallback != null) {
            mRouter.removeCallback(mCallback);
        }
        super.onDestroy();
    }
}
