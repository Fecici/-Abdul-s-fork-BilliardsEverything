package billiards.viewer;

import billiards.codeseq.ClassifiedCodeSequence;
import billiards.cover.Triple;
import billiards.cover.HalfTriple;
import billiards.geometry.Rectangle;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;

import javafx.scene.paint.Color;

public class HashTriple {

    private final MutableMap<Rectangle, ClassifiedCodeSequence> stableMap;
    private final MutableMap<Rectangle, Triple> tripleMap;
    private final MutableMap<Rectangle, Color> colorMap;
    private final MutableMap<Rectangle, HalfTriple> halfTripleMap;

    public HashTriple() {
        stableMap = new UnifiedMap<>();
        tripleMap = new UnifiedMap<>();
        halfTripleMap = new UnifiedMap<>();
        colorMap = new UnifiedMap<>();
    }

    public void addStables(final Map<Rectangle, ClassifiedCodeSequence> otherMap, final Color color) {
        this.stableMap.putAll(otherMap);
        for (final Rectangle rect : otherMap.keySet()) {
            this.colorMap.put(rect, color);
        }
    }

    public void addTriples(final Map<Rectangle, Triple> otherMap, final Color color) {
        this.tripleMap.putAll(otherMap);
        for (final Rectangle rect : otherMap.keySet()) {
            this.colorMap.put(rect, color);
        }
    }
    public void addHalfTriples(final Map<Rectangle, HalfTriple> otherMap, final Color color){
        this.halfTripleMap.putAll(otherMap);
        for (final Rectangle rect : otherMap.keySet()) {
            this.colorMap.put(rect, color);
        }
    }

    public void clear() {
        stableMap.clear();
        tripleMap.clear();
        colorMap.clear();
        halfTripleMap.clear();

    }

    public MutableList<Rectangle> stableEntrySet() {
        final MutableList<Rectangle> entries = new FastList<>();
        stableMap.entrySet().forEach(rect -> entries.add(rect.getKey()));
        return entries;
    }

    public MutableList<Rectangle> tripleEntrySet() {
        final MutableList<Rectangle> entries = new FastList<>();
        tripleMap.entrySet().forEach(rect -> entries.add(rect.getKey()));
        return entries;
    }

    public MutableList<Rectangle> HalfTripleEntrySet() {
        final MutableList<Rectangle> entries = new FastList<>();
        halfTripleMap.entrySet().forEach(rect -> entries.add(rect.getKey()));
        return entries;
    }

    public void remove(Rectangle rect) {
        this.stableMap.remove(rect);
        this.tripleMap.remove(rect);
        this.halfTripleMap.remove(rect);
    }

    public ClassifiedCodeSequence getStable(final Rectangle rect) {
        return stableMap.get(rect);
    }

    public Triple getTriple(final Rectangle rect) {
        return tripleMap.get(rect);
    }

    public HalfTriple getHalfTriple(final Rectangle rect) { return halfTripleMap.get(rect); }

    public Color getColor(final Rectangle rect) {
        return colorMap.get(rect);
    }

    public void put(final Rectangle rect, final ClassifiedCodeSequence stable) {
        stableMap.put(rect, stable);
    }

    public void put(final Rectangle rect, final Triple triple) {
        tripleMap.put(rect, triple);
    }

    public void put(final Rectangle rect, final HalfTriple halfTriple) {
        halfTripleMap.put(rect, halfTriple);
    }

    public void put(final Rectangle rect, final Color color) {
        colorMap.put(rect, color);
    }
}
