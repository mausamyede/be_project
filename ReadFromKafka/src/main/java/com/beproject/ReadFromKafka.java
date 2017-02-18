package com.beproject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer082;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;


public class ReadFromKafka {
	public double runs;
	public double wickets;
	public static ArrayList<Integer> predscores= new ArrayList<Integer>();
	public static ArrayList<Integer> predsegments= new ArrayList<Integer>();
	public ReadFromKafka(double r, double w)
	{
		this.runs=r;
		this.wickets=w;
	}
	static double eucl_dist(double[] x, double[] y)
	{
		double dist=0;
		for(int i=0; i<9; i++)
		{
			dist=dist+Math.pow(x[i]-y[i],2);
		}
		return Math.pow(dist,0.5);
	}
	static ReadFromKafka knn(double[][] train, double[] test, int size_train)
	{
		int num_neigh=5;
		int min_index[]=new int[num_neigh];
		ArrayList<Double> dist_neigh=new ArrayList<Double>();
		for(int i=0; i<size_train; i++)
		{
			dist_neigh.add(eucl_dist(train[i],test));
		}
		//System.out.println(dist_neigh);
		double average_runs=0;
		double average_wicks=0;
		for(int i=0; i<num_neigh; i++)
		{
			min_index[i]=dist_neigh.indexOf(Collections.min(dist_neigh));
			dist_neigh.set(min_index[i],99999.9);
			//System.out.println(min_index[i]);
			average_runs=average_runs+train[min_index[i]][9];
			average_wicks=average_wicks+train[min_index[i]][10];
		}
		return new ReadFromKafka(average_runs/5.0,average_wicks/5);
	}
	public static void main(String[] args) throws Exception {
		// define team key map
		final String teammap[]={"IND","SL","NZ","AUS","PAK","BAN","ENG","WI","SA"};
		// create execution environment
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		// parse user parameters
		ParameterTool parameterTool = ParameterTool.fromArgs(args);

		DataStream<String> messageStream = env.addSource(new FlinkKafkaConsumer082<>(parameterTool.getRequired("topic"), new SimpleStringSchema(), parameterTool.getProperties()));

		// print() will write the contents of the stream to the TaskManager's standard out stream
		// the rebelance call is causing a repartitioning of the data so that all machines
		// see the messages (for example in cases when "num kafka partitions" < "num flink operators"
		messageStream.rebalance().map(new MapFunction<String, String>() {
			private static final long serialVersionUID = -6867736771747690202L;

			@Override
			public String map(String value) throws Exception {
				//String query = "INSERT INTO data1 (data_id, data)"+" VALUES(2,'"+value+"');";
				//Cluster cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
				//Session session = cluster.connect("test");
				//session.execute(query);
				double train_data[][]=new double[1][1];
				int num_lines = 0;
				try (BufferedReader br1 = new BufferedReader(new FileReader("/home/mausam/all_data.txt"))) {
					while (br1.readLine() != null) num_lines++;
					br1.close();
					BufferedReader br = new BufferedReader(new FileReader("/home/mausam/all_data.txt"));
		    			String line;
					int count=0;
					train_data=new double[num_lines][11];
	    			while ((line = br.readLine()) != null) {
	       				String[] parts = line.split(",");
					for(int i=0; i<11; i++)
					{
						train_data[count][i]=Double.parseDouble(parts[i+2].split(":")[1]);
					}
					count++;
	    			}
		    		br.close();
				}
				catch (FileNotFoundException ex)
				{
					System.out.println("File not found!");
				}
				catch (IOException ioex)
				{
					System.out.println("IO exception!");
				}
				//Scanner sc = new Scanner(System.in);
				//System.out.println("Enter test data:");
				String[] values=value.split(",");
				if(values[0].equals("done"))
				{
					File file = new File("/home/mausam/predscores.txt");
			        // creates the file
			        file.createNewFile();
			      
			        // creates a FileWriter Object
			        FileWriter writer = new FileWriter(file); 
			        System.out.println(predscores.size());
			        // Writes the content to the file
			        for(int i=0; i<predscores.size(); i++)
			        {
			        	int score=(int)predscores.get(i);
			        	int seg=(int)predsegments.get(i);
			        	//System.out.println(score);
			        	writer.write(seg+","+score+"\n");
			        	writer.flush();
			        }
			        writer.close();
					System.exit(0);
				}
				double test_data[]=new double[10];
				for(int i=0; i<10; i++)
				{
					test_data[i]=Double.parseDouble(values[i+1].split(":")[1]);
				}
				//System.out.println("Enter the start segment:");
				//int start_seg=Integer.parseInt(args[0]);
				String team=teammap[(int)test_data[0]];
				int start_seg=Integer.parseInt(values[0].split(":")[1]);
				double runs_eoi=test_data[2]+test_data[7];
				double wicks_eoi=test_data[3]+test_data[8];
				int runs=(int)test_data[2]+(int)test_data[7];
				int wickets=(int)test_data[3]+(int)test_data[8];
				for(int i=start_seg; i<=10; i++)
				{
					ReadFromKafka res=knn(train_data,test_data,num_lines);
					//System.out.println("Predicted runs for segment "+(i)+": "+res.runs);
					//System.out.println("Predicted wickets for segment "+(i)+": "+(int)(res.wickets+1));
					test_data[2]=test_data[2]+test_data[7];
					test_data[3]=test_data[3]+test_data[8];
					double target=test_data[9];
					test_data[7]=res.runs;
					test_data[8]=res.wickets;
					runs_eoi=runs_eoi+res.runs;
					wicks_eoi=wicks_eoi+(int)(res.wickets+1);
					if(target!=0 && runs_eoi>=target)
					{
						runs_eoi=target;
						break;
					}
					if(wicks_eoi>=9)
						break;
				}
				//System.out.println("EOI runs: "+runs_eoi);  
				String command = "curl -i http://localhost:5000/social_media/handler/Current%20score:"+runs+"-"+wickets+"%0APredicted%20EOI%20runs:"+(int)runs_eoi+"/1";
				Runtime.getRuntime().exec(command);
				predsegments.add(start_seg);
				predscores.add((int)runs_eoi);
				System.out.println(predscores);
				
				return "Current score for "+team+" : "+runs+"/"+wickets+". Predicted EOI score: "+(int)runs_eoi;
			}			
		}).print();
		env.execute();
	}
}
