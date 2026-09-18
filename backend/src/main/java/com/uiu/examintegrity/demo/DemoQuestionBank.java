package com.uiu.examintegrity.demo;

import java.util.List;

final class DemoQuestionBank {
    private DemoQuestionBank() {
    }

    record Item(
            String topic,
            String bloomLevel,
            short difficulty,
            String body,
            String explanation,
            List<String> options,
            int correctIndex) {
    }

    static final List<Item> ITEMS = List.of(
            new Item("patterns", "Remember", (short) 1,
                    "Which pattern lets you add behaviour to a single object without affecting other instances of the same class?",
                    "Decorator wraps one instance. Subclassing would change every instance of the subclass, and a Strategy swaps an algorithm rather than layering on behaviour.",
                    List.of("Decorator", "Singleton", "Factory Method", "Observer"), 0),

            new Item("patterns", "Understand", (short) 3,
                    "A class needs to notify an unknown number of interested objects when its state changes, without knowing their types. Which pattern applies?",
                    "Observer: the subject holds a list of listeners it knows only through an interface, so new listener types need no change to the subject.",
                    List.of("Observer", "Adapter", "Builder", "Proxy"), 0),

            new Item("generics", "Understand", (short) 3,
                    "What does the wildcard in List<? extends Number> allow?",
                    "You may read elements as Number, but you may not add anything except null -- the compiler cannot know which subtype the list actually holds.",
                    List.of(
                            "Reading elements as Number, but not adding any",
                            "Adding any subclass of Number",
                            "Both reading and adding Number values",
                            "Neither reading nor adding"),
                    0),

            new Item("generics", "Analyze", (short) 4,
                    "Why does a generic type parameter disappear at runtime in Java?",
                    "Type erasure. Generics are checked by the compiler and then erased, which is what keeps generic code binary-compatible with pre-generics classes.",
                    List.of(
                            "Type erasure keeps bytecode compatible with older code",
                            "The JVM stores type arguments in the constant pool",
                            "Generic types are resolved lazily on first use",
                            "The garbage collector discards unused type metadata"),
                    0),

            new Item("collections", "Remember", (short) 2,
                    "Which collection guarantees iteration in the order elements were inserted?",
                    "LinkedHashMap keeps a linked list through its entries. HashMap makes no ordering promise and TreeMap orders by key, not by insertion.",
                    List.of("LinkedHashMap", "HashMap", "TreeMap", "Hashtable"), 0),

            new Item("collections", "Apply", (short) 3,
                    "You override equals() on a class used as a HashMap key but leave hashCode() alone. What happens?",
                    "Two objects that are equal can land in different buckets, so a lookup with an equal key misses. equals() and hashCode() have to be overridden together.",
                    List.of(
                            "Lookups may fail to find an equal key",
                            "The map throws IllegalStateException",
                            "Nothing changes; the map uses equals() only",
                            "Keys are silently replaced on every put"),
                    0),

            new Item("exceptions", "Understand", (short) 2,
                    "What distinguishes a checked exception from an unchecked one?",
                    "A checked exception must be declared or handled, which is the compiler forcing a decision about a failure the caller is expected to anticipate.",
                    List.of(
                            "Checked exceptions must be declared or caught",
                            "Checked exceptions cannot be subclassed",
                            "Unchecked exceptions cannot be caught",
                            "Unchecked exceptions always terminate the JVM"),
                    0),

            new Item("exceptions", "Analyze", (short) 5,
                    "A finally block contains a return statement and the try block also returns. Which value reaches the caller?",
                    "The value from finally wins and the try block's return is discarded -- which is exactly why returning from finally is considered a defect.",
                    List.of(
                            "The value returned from finally",
                            "The value returned from try",
                            "Whichever executed first",
                            "The code does not compile"),
                    0),

            new Item("concurrency", "Apply", (short) 4,
                    "Two threads increment a shared int counter with the ++ operator and no synchronization. What is the risk?",
                    "++ is a read-modify-write, not one atomic step. Two threads can read the same value and both write back the same increment, losing one.",
                    List.of(
                            "Lost updates, because ++ is not atomic",
                            "Nothing; ++ is atomic for int",
                            "A deadlock between the two threads",
                            "An immediate ConcurrentModificationException"),
                    0),

            new Item("concurrency", "Analyze", (short) 5,
                    "What does declaring a field volatile guarantee?",
                    "Visibility and ordering: a write is seen by every later read. It does not make compound operations such as ++ atomic -- that needs a lock or an atomic type.",
                    List.of(
                            "Visibility of writes across threads, but not atomicity",
                            "Atomicity of compound operations such as ++",
                            "That only one thread may touch the field",
                            "That the field is never cached in main memory"),
                    0));

    record WrittenItem(
            String topic,
            String bloomLevel,
            short difficulty,
            String body,
            String explanation,
            List<String> openers,
            List<List<String>> ideas,
            List<String> examples) {
    }

    static final List<WrittenItem> WRITTEN = List.of(
            new WrittenItem("patterns", "Evaluate", (short) 4,
                    "Explain why composition is often preferred over inheritance when designing a class hierarchy.",
                    "Composition keeps coupling low and lets behaviour change at runtime; inheritance fixes it at compile time and exposes the parent's internals to every subclass.",
                    List.of(
                            "Composition is usually the better default.",
                            "In most designs I would reach for composition first.",
                            "Favouring composition tends to give more flexible code.",
                            "The main argument for composition is flexibility.",
                            "Inheritance looks convenient but composition ages better."),
                    List.of(
                            List.of("Inheritance ties the subclass to the parent implementation, so a change in the parent can break every child.",
                                    "A subclass depends on how its parent is implemented, and parent changes ripple down.",
                                    "With inheritance the child is coupled to the internals of the base class."),
                            List.of("With composition the behaviour can be swapped at runtime by giving the object a different collaborator.",
                                    "Composed objects can change behaviour while the program runs by replacing a component.",
                                    "You can plug in a different implementation at runtime when behaviour is composed."),
                            List.of("Deep hierarchies become hard to understand and every level adds hidden assumptions.",
                                    "Long inheritance chains are difficult to follow and easy to break.",
                                    "Class hierarchies grow deep and the reasoning gets harder at every level."),
                            List.of("Composition makes unit testing easier because a collaborator can be replaced with a stub.",
                                    "Testing is simpler since each composed part can be mocked on its own.",
                                    "Each component can be tested in isolation when you compose instead of inherit."),
                            List.of("It also avoids the fragile base class problem.",
                                    "This sidesteps the fragile base class issue entirely.",
                                    "The fragile base class problem mostly disappears."),
                            List.of("Java only allows single inheritance, so composition is the only way to reuse several behaviours.",
                                    "Since a class can extend just one parent, combining behaviours needs composition.",
                                    "Single inheritance limits reuse, while composition can combine any number of parts.")),
                    List.of(
                            "For example a Car has an Engine rather than being an Engine.",
                            "A logger that holds a formatter is a typical example.",
                            "Think of a Stack built on a List instead of extending it.",
                            "The Strategy pattern is composition applied deliberately.",
                            "A GUI button that holds a click handler shows the idea.",
                            "Collections.unmodifiableList wraps a list rather than subclassing it.")),

            new WrittenItem("concurrency", "Analyze", (short) 4,
                    "Two threads update a shared bank balance. Describe what can go wrong and how you would prevent it.",
                    "The read-modify-write is not atomic, so updates can be lost. Guard it with a lock or synchronized block, or use an atomic type.",
                    List.of(
                            "This is a classic race condition.",
                            "The problem here is a race between the two threads.",
                            "Without coordination the balance can end up wrong.",
                            "Shared mutable state is the root of the issue.",
                            "Both threads touching the balance at once is unsafe."),
                    List.of(
                            List.of("Reading the balance, adding to it and writing it back is three steps, and another thread can run in between.",
                                    "The update is a read, a change and a write, and the threads can interleave between them.",
                                    "Balance plus amount is not one operation, so the threads can overlap inside it."),
                            List.of("One thread's deposit can overwrite the other one, so money is lost.",
                                    "An update can be lost when both threads write back a stale value.",
                                    "The final balance may miss one of the updates entirely."),
                            List.of("Wrapping the update in a synchronized block makes it happen one thread at a time.",
                                    "A synchronized method or a lock around the update fixes it.",
                                    "Guarding the balance with a ReentrantLock serialises the updates."),
                            List.of("An AtomicLong with addAndGet avoids locking altogether.",
                                    "Using an atomic type such as AtomicLong makes the increment atomic.",
                                    "Atomic classes do the read-modify-write in a single step."),
                            List.of("volatile alone is not enough because it gives visibility but not atomicity.",
                                    "Marking the field volatile would not help since the increment is still not atomic.",
                                    "volatile fixes visibility only, so it does not prevent lost updates."),
                            List.of("The bug may not show up in testing because it depends on timing.",
                                    "Races are timing dependent, so tests can pass while the bug is still there.",
                                    "It can work thousands of times and fail rarely, which makes it hard to find.")),
                    List.of(
                            "For example two deposits of 100 can leave the balance only 100 higher.",
                            "If both read 500 and add 50, the result is 550 instead of 600.",
                            "Two ATMs withdrawing at once is the textbook case.",
                            "A payroll batch running beside a user transfer could trigger it.",
                            "An online shop updating stock from two requests has the same bug.",
                            "A counter of page views shows the same lost update.")));
}
