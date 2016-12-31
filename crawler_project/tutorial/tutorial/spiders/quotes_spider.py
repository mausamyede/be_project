import scrapy
import os

class MySpider(scrapy.Spider):
	name="cricbuzz"

	def __init__(self, *args, **kwargs):
		super(MySpider, self).__init__(*args, **kwargs)
		self.start_urls = [kwargs.get('start_url')]
	#start_urls=['http://www.cricbuzz.com/live-cricket-scores/16475/aus-vs-rsa-1st-test-south-africa-tour-of-australia-2016']
	
	def parse(self, response):
		#filename='currmatch.html'
		batsnbowl=response.xpath('//div[@class="cb-col cb-col-50"]/a/text()').extract()
		sentence=""
		#f=open('currmatch.html', 'wb')
		for name in batsnbowl:
			#f.write("".join(name)+"\n")
			sentence=sentence+"".join(name)+","
		os.system('echo "'+sentence+'" | ~/Downloads/kafka/bin/kafka-console-producer.sh --broker-list localhost:9092 --topic TutorialTopic > /dev/null')
			
