package myapps;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.*;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyProcessor extends AbstractProcessor<String, String> {
    private ProcessorContext context;
    private KeyValueStore<String, ArrayList<Long>> kvStore;

    @Override
    @SuppressWarnings("unchecked")
    public void init(ProcessorContext context) {
// keep the processor context locally because we need it in punctuate() and commit()
        this.context = context;

// schedule a punctuation method every 1000 milliseconds.
        this.context.schedule(1000, PunctuationType.WALL_CLOCK_TIME, new Punctuator() {
            @Override
            public void punctuate(long timestamp) {
                KeyValueIterator<String, ArrayList<Long>> iter = kvStore.all();

                // [(word, timestamp), (word, timestamp), (word, timestamp)...]

                while (iter.hasNext()) {
                    KeyValue<String, ArrayList<Long>> entry = iter.next();
                    Long currentTimeMS = System.currentTimeMillis();
                    if (currentTimeMS - entry.value.get(entry.value.size() - 1) >= 10000) {
                        kvStore.delete(entry.key);
                        context.forward(entry.key, entry.value.toString());
                    }
                }

                // it is the caller's responsibility to close the iterator on state store;
                // otherwise it may lead to memory and file handlers leak depending on the
                // underlying state store implementation.
                iter.close();

                // commit the current processing progress
                context.commit();
            }
        });

// retrieve the key-value store named "Counts"
        this.kvStore = (KeyValueStore<String, ArrayList<Long>>) context.getStateStore("Counts");
    }

    @Override
    public void process(String key, String tsString) {
        ArrayList<Long> oldValue = this.kvStore.get(key);

        Long timestamp = Long.parseLong(tsString);

        if (oldValue == null) {
            ArrayList<Long> l = new ArrayList<Long>(Arrays.asList(timestamp));
            this.kvStore.put(key, l);
        } else {
            oldValue.add(timestamp);
            this.kvStore.put(key, oldValue);
        }
    }

//    @Override
//    public void punctuate(long timestamp) {
//        KeyValueIteratorator<String, Long> iter = this.kvStore.all();
//
//        while (iter.hasNext()) {
//            KeyValue<S tring, Long> entry = iter.next();
//            context.forward(entry.key, entry.value.toString());
//        }
//
//        iter.close(); // avoid OOM
//// commit the current processing progress
//        context.commit();
//    }

    @Override
    public void close() {
// close any resources managed by this processor.
// Note: Do not close any StateStores as these are managed
// by the library
    }
};