/**************************************************************************
 OSMemory library for OSM data processing.

 Copyright (C) 2014 Aleś Bułojčyk <alex73mail@gmail.com>
               Home page: http://www.omegat.org/
               Support center: http://groups.yahoo.com/group/OmegaT/

 This is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This software is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.alex73.osmemory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Storage for all nodes, ways, relations.
 */
public class MemoryStorage2 {
    static final Pattern RE_OBJECT_CODE = Pattern.compile("([nwr])([0-9]+)");

    final protected List<NodeObject2> nodes = new ArrayList<>();
    final protected List<WayObject2> ways = new ArrayList<>();
    final protected List<RelationObject2> relations = new ArrayList<>();

    final private StringPack tagsPack = new StringPack();
    final private StringPack relationRolesPack = new StringPack();

    private long loadingStartTime, loadingFinishTime;

    public MemoryStorage2() {
        loadingStartTime = System.currentTimeMillis();
    }

    void finishLoading() throws Exception {
        // check ID order
        long prev = 0;
        for (int i = 0; i < nodes.size(); i++) {
            long id = nodes.get(i).id;
            if (id < prev) {
                throw new Exception("Nodes must be ordered by ID");
            }
        }
        prev = 0;
        for (int i = 0; i < ways.size(); i++) {
            long id = ways.get(i).id;
            if (id < prev) {
                throw new Exception("Ways must be ordered by ID");
            }
        }
        prev = 0;
        for (int i = 0; i < relations.size(); i++) {
            long id = relations.get(i).id;
            if (id < prev) {
                throw new Exception("Relations must be ordered by ID");
            }
        }
        loadingFinishTime = System.currentTimeMillis();
    }

    public StringPack getTagsPack() {
        return tagsPack;
    }

    public StringPack getRelationRolesPack() {
        return relationRolesPack;
    }

    private static <T extends BaseObject2> T getById(List<T> en, long id) {
        int low = 0;
        int high = en.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midvalue = en.get(mid).id;

            if (midvalue < id)
                low = mid + 1;
            else if (midvalue > id)
                high = mid - 1;
            else
                return en.get(mid);
        }
        return null;
    }

    public NodeObject2 getNodeById(long id) {
        return getById(nodes, id);
    }

    public WayObject2 getWayById(long id) {
        return getById(ways, id);
    }

    public RelationObject2 getRelationById(long id) {
        return getById(relations, id);
    }

    /**
     * Get object by code like n123, w456, r789.
     */
    public BaseObject2 getObject(String code) throws Exception {
        Matcher m = RE_OBJECT_CODE.matcher(code.trim());
        if (!m.matches()) {
            throw new Exception("Няправільны фарматы code: " + code);
        }
        long idl = Long.parseLong(m.group(2));
        switch (m.group(1)) {
        case "n":
            return getNodeById(idl);
        case "w":
            return getWayById(idl);
        case "r":
            return getRelationById(idl);
        default:
            throw new Exception("Няправільны фарматы code: " + code);
        }
    }

    public void showStat() {
        System.out.println("Loading time    : "
                + new DecimalFormat(",##0").format((loadingFinishTime - loadingStartTime)) + "ms");
        System.out.println("Nodes count     : " + new DecimalFormat(",##0").format(nodes.size()));
        System.out.println("Ways count      : " + new DecimalFormat(",##0").format(ways.size()));
        System.out.println("Relations count : " + new DecimalFormat(",##0").format(relations.size()));
    }

    public String getTag(BaseObject2 obj, String tagName) {
        short tagKey = tagsPack.getTagCode(tagName);
        return obj.getTagValue(tagKey);
    }

    public Map<String, String> extractTags(BaseObject2 obj) {
        Map<String, String> result = new TreeMap<>();
        for (int i = 0; i < obj.tagKeys.length; i++) {
            String tagName = tagsPack.getTagName(obj.tagKeys[i]);
            result.put(tagName, obj.tagValues[i]);
        }
        return result;
    }

    public AllProcessor allHasTag(String tagName) {
        List<BaseObject2> list = new ArrayList<>();
        short tagKey = tagsPack.getTagCode(tagName);
        for (int i = 0; i < nodes.size(); i++) {
            NodeObject2 n = nodes.get(i);
            if (n.hasTag(tagKey)) {
                list.add(n);
            }
        }
        for (int i = 0; i < ways.size(); i++) {
            WayObject2 n = ways.get(i);
            if (n.hasTag(tagKey)) {
                list.add(n);
            }
        }
        for (int i = 0; i < relations.size(); i++) {
            RelationObject2 n = relations.get(i);
            if (n.hasTag(tagKey)) {
                list.add(n);
            }
        }
        return new AllProcessor(list);
    }

    public AllProcessor all() {
        List<BaseObject2> list = new ArrayList<>(nodes.size() + ways.size() + relations.size());
        list.addAll(nodes);
        list.addAll(ways);
        list.addAll(relations);
        return new AllProcessor(list);
    }

    public class AllProcessor {
        private final List<BaseObject2> list;

        private AllProcessor(List<BaseObject2> list) {
            this.list = list;
        }

        public void process(Predicate<BaseObject2> predicate, Consumer<BaseObject2> consumer) {
            for (int i = 0; i < list.size(); i++) {
                BaseObject2 n = list.get(i);
                if (predicate.test(n)) {
                    consumer.accept(n);
                }
            }
        }

        public void processAll(Consumer<BaseObject2> consumer) {
            for (int i = 0; i < list.size(); i++) {
                consumer.accept(list.get(i));
            }
        }
    }
}
