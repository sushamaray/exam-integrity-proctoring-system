package com.uiu.examintegrity.similarity.domain;

import java.util.HashMap;
import java.util.Map;

public final class UnionFind {
    private final Map<Long, Long> parent = new HashMap<>();
    private final Map<Long, Integer> size = new HashMap<>();

    public Long find(Long x) {
        parent.putIfAbsent(x, x);
        size.putIfAbsent(x, 1);
        Long root = x;
        while (!parent.get(root).equals(root)) {
            root = parent.get(root);
        }

        Long node = x;
        while (!parent.get(node).equals(root)) {
            Long next = parent.get(node);
            parent.put(node, root);
            node = next;
        }
        return root;
    }

    public void union(Long a, Long b) {
        Long ra = find(a);
        Long rb = find(b);
        if (ra.equals(rb)) {
            return;
        }
        if (size.get(ra) < size.get(rb)) {
            Long swap = ra;
            ra = rb;
            rb = swap;
        }
        parent.put(rb, ra);
        size.put(ra, size.get(ra) + size.get(rb));
    }

    public int sizeOf(Long x) {
        return size.get(find(x));
    }
}
