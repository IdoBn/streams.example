package myapps;

import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.apache.kafka.streams.processor.StateStoreSupplier;
import org.apache.kafka.streams.processor.TopologyBuilder;
import org.apache.kafka.streams.state.KeyValueBytesStoreSupplier;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class MyProcessorDriver {
    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-linesplit");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        KeyValueBytesStoreSupplier countStoreSupplier = Stores.inMemoryKeyValueStore("Counts");
        StoreBuilder builder = Stores.keyValueStoreBuilder(countStoreSupplier,
                Serdes.String(),
                new ArrayListSerde<>(Serdes.Long()))
                .withLoggingDisabled();

        Topology topology = new Topology();
        topology
                .addSource("SOURCE", Serdes.String().deserializer(), Serdes.String().deserializer(), "streams-plaintext-input")
                .addProcessor("PROCESS1", () -> new MyProcessor(), "SOURCE")
                .addSink("SINK1", "streams-linesplit-output", "PROCESS1")
                .addStateStore(builder, "PROCESS1");


        KafkaStreams streaming = new KafkaStreams(topology, props);

        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streaming.close();
                latch.countDown();
            }
        });

        try {
            streaming.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }
}
