/**************************************************************************
 Some tools for OSM.

 Copyright (C) 2013 Aleś Bułojčyk <alex73mail@gmail.com>
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

package org.alex73.osm.daviednik;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.alex73.osm.validators.vulicy2.OsmPlace;
import org.apache.ibatis.session.SqlSession;

/**
 * Вызначае правільныя тэгі для назваў населеных пунктаў.
 */
public class CalcCorrectTags {
    static public OsmPlace calc(Miesta m, SqlSession db) throws Exception {
        String typ;
        switch (m.typ) {
        case "г.":
            if (m.osmID == null) {
                throw new Exception("Node ID не вызначана для горада: " + m);
            }
            OsmPlace n = db.selectOne("getPlaceById", m.osmID);
            if (n == null) {
                throw new Exception("Няма горада: " + m);
            }
            String population = n.population;
            if (population != null && Integer.parseInt(population) >= 50000) {
                typ = "city";
            } else {
                typ = "town";
            }
            break;
        case "г.п.":
        case "г. п.":
            typ = "village";
            break;
        case "в.":
        case "п.":
        case "р.п.":
        case "р. п.":
        case "аг.":
            typ = "hamlet";
            break;
        case "х.":
            typ = "isolated_dwelling";
            break;
        case "ст.":
        case "с.":
        case "раз’езд":
            typ = null;
            break;
        default:
            throw new RuntimeException(m.typ + " for " + m.osmID);
        }

        String name_be = m.nazvaNoStress;
        String name_be_tarask = unstress(m.osmNameBeTarask);
        String variants_be = variantsToString(splitVariants(m.varyjantyBel));
        String int_name = m.translit;

        String name_ru = m.osmForceNameRu != null ? m.osmForceNameRu : m.ras;
        List<String> vru = new ArrayList<>();
        sadd(vru, m.rasUsedAsOld);
        sadd(vru, m.ras);
        sadd(vru, m.osmAltNameRu);
        sdel(vru, name_ru);
        String variants_ru = variantsToString(vru);

        OsmPlace result = new OsmPlace();
        result.name= name_ru;
        result.name_ru= name_ru;
        result.name_be= name_be;
        result.int_name=int_name;
        result.name_be_tarask= name_be_tarask;
        if (typ != null) {
            result.place= typ;
        }
        result.alt_name_ru= variants_ru;
        result.alt_name_be= variants_be;
        result.alt_name= null;

        return result;
    }

    static String variantsToString(List<String> variants) {
        if (variants.isEmpty()) {
            return null;
        }
        StringBuilder out = new StringBuilder(200);
        for (String v : variants) {
            out.append(';').append(v);
        }
        return out.substring(1).toString();
    }

    static String unstress(String s) {
        if (s == null) {
            return null;
        }
        return s.replace("\u0301", "");
    }

    static List<String> splitVariants(String s) {
        if (s == null) {
            return Collections.emptyList();
        }
        s = unstress(s).replaceAll("\\(.+\\)", "");
        List<String> out = new ArrayList<>();
        for (String w : s.split("[,;]")) {
            w = w.trim();
            if (w.startsWith("пад ") || w.startsWith("за ") || w.startsWith("з ")) {
                continue;
            }
            switch (w) {
            case "м.":
            case "ж.":
            case "н.":
            case "мн.":
                break;
            default:
                out.add(w);
            }
        }
        return out;
    }

    static void sadd(List<String> set, String add) {
        if (add != null && !set.contains(add)) {
            set.add(add);
        }
    }

    static void sdel(List<String> set, String del) {
        if (del != null) {
            set.remove(del);
        }
    }
}
