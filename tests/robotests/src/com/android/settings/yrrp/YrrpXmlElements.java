/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.yrrp;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.provider.SearchIndexableResource;

import androidx.annotation.XmlRes;

import com.android.settings.search.BaseSearchIndexProvider;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a compiled preference XML into an ordered element list, so structure tests can assert the
 * exact tags, nesting and attributes a page ships with. Key-only checks use {@link
 * com.android.settings.testutils.XmlTestUtils#getKeysFromPreferenceXml} instead.
 */
final class YrrpXmlElements {
    private YrrpXmlElements() {}

    /** One start tag with its attributes, keyed by attribute name without namespace. */
    static final class Element {
        final String mTag;
        final int mDepth;
        private final Map<String, String> mValues = new HashMap<>();
        private final Map<String, Integer> mResourceIds = new HashMap<>();

        private Element(XmlResourceParser parser) {
            mTag = parser.getName();
            mDepth = parser.getDepth();
            for (int i = 0; i < parser.getAttributeCount(); i++) {
                final String name = parser.getAttributeName(i);
                mValues.put(name, parser.getAttributeValue(i));
                mResourceIds.put(name, parser.getAttributeResourceValue(i, 0));
            }
        }

        String key() {
            return value("key");
        }

        /** The attribute's literal or coerced string value, or null when absent. */
        String value(String name) {
            return mValues.get(name);
        }

        /** The attribute's resource reference, or 0 when absent or not a reference. */
        int resourceId(String name) {
            final Integer id = mResourceIds.get(name);
            return id == null ? 0 : id;
        }

        int order() {
            return Integer.parseInt(value("order"));
        }

        /** {@code tag#key}, indented two spaces per nesting level below the root. */
        String outline() {
            return "  ".repeat(mDepth - 1) + mTag + "#" + key();
        }
    }

    /** Every start tag of {@code xmlResId} in document order. */
    static List<Element> read(Context context, @XmlRes int xmlResId)
            throws IOException, XmlPullParserException {
        final List<Element> elements = new ArrayList<>();
        try (XmlResourceParser parser = context.getResources().getXml(xmlResId)) {
            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG) {
                    elements.add(new Element(parser));
                }
            }
        }
        return elements;
    }

    /** The outline of every element, in document order. */
    static List<String> outline(Context context, @XmlRes int xmlResId)
            throws IOException, XmlPullParserException {
        final List<String> outline = new ArrayList<>();
        for (Element element : read(context, xmlResId)) {
            outline.add(element.outline());
        }
        return outline;
    }

    /** The first element with {@code key}; fails when there is none. */
    static Element find(List<Element> elements, String key) {
        for (Element element : elements) {
            if (key.equals(element.key())) {
                return element;
            }
        }
        throw new AssertionError("No element with key " + key);
    }

    /** The elements nested inside the element with {@code key}, in document order. */
    static List<Element> children(List<Element> elements, String key) {
        final Element parent = find(elements, key);
        final List<Element> children = new ArrayList<>();
        for (int i = elements.indexOf(parent) + 1; i < elements.size(); i++) {
            final Element element = elements.get(i);
            if (element.mDepth <= parent.mDepth) {
                break;
            }
            children.add(element);
        }
        return children;
    }

    /** The XML resources a search provider indexes. */
    static List<Integer> indexedXml(Context context, BaseSearchIndexProvider provider) {
        final List<Integer> xmlResIds = new ArrayList<>();
        for (SearchIndexableResource resource :
                provider.getXmlResourcesToIndex(context, true /* enabled */)) {
            xmlResIds.add(resource.xmlResId);
        }
        return xmlResIds;
    }
}
