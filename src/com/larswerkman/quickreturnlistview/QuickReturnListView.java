/*
 * Copyright 2013 Lars Werkman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.larswerkman.quickreturnlistview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ListView;

public class QuickReturnListView extends ListView {

    private int mItemCount;
    private int mItemOffsetY[];
    private boolean scrollIsComputed = false;
    private int mHeight;

    private View mStickyView;
    private View mPlaceHolderView;

    private int mCachedVerticalScrollRange;
    @SuppressWarnings("unused")
    private int mQuickReturnHeight;

    private int mScrollY;

    private TranslateAnimation mAnim;

    private int mStickyViewResourceId;
    private int mPlaceHolderViewResourceId;

    public QuickReturnListView(Context context) {
        this(context, null);
    }

    public QuickReturnListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QuickReturnListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray ta = context.obtainStyledAttributes(attrs,
                R.styleable.QuickReturnListView);
        mStickyViewResourceId = ta.getResourceId(
                R.styleable.QuickReturnListView_viewSticky, -1);
        mPlaceHolderViewResourceId = ta.getResourceId(
                R.styleable.QuickReturnListView_viewPlaceholder, -1);
        ta.recycle();

        init();
    }

    private void init() {
        getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mStickyView = getRootView().findViewById(
                                mStickyViewResourceId);
                        mPlaceHolderView = getRootView().findViewById(
                                mPlaceHolderViewResourceId);
                        ViewGroup parentView = (ViewGroup) getParent();
                        if (mPlaceHolderView == null || mStickyView == null
                                || mPlaceHolderView.equals(mStickyView)
                                || !(parentView instanceof FrameLayout)) {
                            throw new IllegalArgumentException(
                                    "placeHolderView and stickyView must be set, and must be not the same view, and QuickReturnListView'parent must be FrameLayout");
                        }
                        if (parentView.findViewById(mStickyViewResourceId) == null
                                || parentView
                                        .findViewById(mPlaceHolderViewResourceId) == null) {
                            throw new IllegalArgumentException(
                                    "placeHolderView and stickyView must be Framelayout's child view");
                        }
                        mQuickReturnHeight = mStickyView.getHeight();
                        computeScrollY();
                        mCachedVerticalScrollRange = getListHeight();
                    }
                });

        setOnScrollListener(new OnScrollListener() {
            @SuppressLint("NewApi")
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount) {
                if (mPlaceHolderView == null || mStickyView == null) {
                    return;
                }
                mScrollY = 0;
                int translationY = 0;

                if (scrollYIsComputed()) {
                    mScrollY = getComputedScrollY();
                }

                int rawY = mPlaceHolderView.getTop()
                        - Math.min(mCachedVerticalScrollRange - getHeight(),
                                mScrollY);

                if (rawY < 0) {
                    translationY = 0;
                } else if (rawY < mPlaceHolderView.getTop()) {
                    translationY = rawY;
                } else {
                    translationY = mPlaceHolderView.getTop();
                }

                /** this can be used if the build is below honeycomb **/
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.HONEYCOMB) {
                    mAnim = new TranslateAnimation(0, 0, translationY,
                            translationY);
                    mAnim.setFillAfter(true);
                    mAnim.setDuration(0);
                    mStickyView.startAnimation(mAnim);
                } else {
                    mStickyView.setTranslationY(translationY);
                }

            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }
        });
    }

    public int getListHeight() {
        return mHeight;
    }

    public void computeScrollY() {
        mHeight = 0;
        mItemCount = getAdapter().getCount();
        if (mItemOffsetY == null) {
            mItemOffsetY = new int[mItemCount];
        }
        for (int i = 0; i < mItemCount; ++i) {
            View view = getAdapter().getView(i, null, this);
            view.measure(
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            mItemOffsetY[i] = mHeight;
            mHeight += view.getMeasuredHeight();
        }
        scrollIsComputed = true;
    }

    public boolean scrollYIsComputed() {
        return scrollIsComputed;
    }

    public int getComputedScrollY() {
        int pos, nScrollY, nItemY;
        View view = null;
        pos = getFirstVisiblePosition();
        view = getChildAt(0);
        nItemY = view.getTop();
        nScrollY = mItemOffsetY[pos] - nItemY;
        return nScrollY;
    }
}