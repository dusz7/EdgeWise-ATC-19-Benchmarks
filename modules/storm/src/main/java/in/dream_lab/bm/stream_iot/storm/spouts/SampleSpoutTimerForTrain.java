package in.dream_lab.bm.stream_iot.storm.spouts;

import in.dream_lab.bm.stream_iot.storm.genevents.EventGen;
import in.dream_lab.bm.stream_iot.storm.genevents.ISyntheticEventGen;
import in.dream_lab.bm.stream_iot.storm.genevents.logging.BatchedFileLogging;
import in.dream_lab.bm.stream_iot.storm.genevents.utils.GlobalConstants;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SampleSpoutTimerForTrain extends BaseRichSpout implements ISyntheticEventGen {
	SpoutOutputCollector _collector;
	EventGen eventGen;
	BlockingQueue<List<String>> eventQueue;
	String csvFileName;
	String outSpoutCSVLogFileName;
	String experiRunId;
	double scalingFactor;
	BatchedFileLogging ba;
	long msgId;
	int inputRate;
	long numEvents;
	long startingMsgId;

	public SampleSpoutTimerForTrain() {
		// this.csvFileName = "/home/ubuntu/sample100_sense.csv";
		// System.out.println("Inside sample spout code");
		this.csvFileName = "/home/tarun/j2ee_workspace/eventGen-anshu/eventGen/bangalore.csv";
		this.scalingFactor = GlobalConstants.accFactor;
		// System.out.print("the output is as follows");
	}

	public SampleSpoutTimerForTrain(String csvFileName, String outSpoutCSVLogFileName, double scalingFactor,
			String experiRunId) {
		this.csvFileName = csvFileName;
		this.outSpoutCSVLogFileName = outSpoutCSVLogFileName;
		this.scalingFactor = scalingFactor;
		this.experiRunId = experiRunId;
	}

	public SampleSpoutTimerForTrain(String csvFileName, String outSpoutCSVLogFileName, double scalingFactor) {
		this(csvFileName, outSpoutCSVLogFileName, scalingFactor, "");
	}

	public SampleSpoutTimerForTrain(String csvFileName, String outSpoutCSVLogFileName, double scalingFactor,
			int inputRate, long numEvents) {
		this(csvFileName, outSpoutCSVLogFileName, scalingFactor, "");
		this.inputRate = inputRate;
		this.numEvents = numEvents;
	}

	@Override
	public void nextTuple() {

		int count = 0, MAX_COUNT = 1;

		while (count < MAX_COUNT) {
			List<String> entry = this.eventQueue.poll();

			if (entry == null || (this.msgId > this.startingMsgId + this.numEvents))
				return;

			count++;
			Values values = new Values();
			StringBuilder rowStringBuf = new StringBuilder();
			for (String s : entry) {
				rowStringBuf.append(",").append(s);
			}
			String rowString = rowStringBuf.toString().substring(1);
			String ROWKEYSTART = rowString.split(",")[2];
			String ROWKEYEND = rowString.split(",")[3];

			values.add(rowString);
			msgId++;
			values.add(Long.toString(msgId));
			values.add(ROWKEYSTART);
			values.add(ROWKEYEND);

			this._collector.emit(values);

			try {
				ba.batchLogwriter(System.currentTimeMillis(), "MSGID," + msgId);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void open(Map map, TopologyContext context, SpoutOutputCollector collector) {
		// TODO Auto-generated method stub

		BatchedFileLogging.writeToTemp(this, this.outSpoutCSVLogFileName);
		Random r = new Random();

		try {
			msgId = (long) (1 * Math.pow(10, 12) + (r.nextInt(1000) * Math.pow(10, 9)) + r.nextInt(10));
			this.startingMsgId = msgId;
		} catch (Exception e) {

			e.printStackTrace();
		}

		_collector = collector;
		this.eventGen = new EventGen(this, this.scalingFactor, this.inputRate);
		this.eventQueue = new LinkedBlockingQueue<List<String>>();
		String uLogfilename = this.outSpoutCSVLogFileName;
		this.eventGen.launch(this.csvFileName, uLogfilename); // Launch threads

		ba = new BatchedFileLogging(uLogfilename, context.getThisComponentId());
	}

	@Override
	public void ack(Object msgId) {
		System.out.println("Acker called");
	}

	@Override
	public void fail(Object msgId) {
		System.out.println("Failure called");
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		// TODO Auto-generated method stub
		// List<String> fieldsList = EventGen.getHeadersFromCSV(csvFileName);
		// fieldsList.add("MSGID");
		// declarer.declare(new Fields(fieldsList));
		declarer.declare(new Fields("RowString", "MSGID", "ROWKEYSTART", "ROWKEYEND"));
	}

	@Override
	public void receive(List<String> event) {
		;
		try {
			this.eventQueue.put(event);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

// use for taxi -
// inputFileForTimerSpout-TAXI.csv
