# Recommendation System

Please refer the blogs for reference.

#### File description: 

Code in `/src/main/java/com/arango/spark/instacart/`

1. `Aisle.java`,`Department.java`,`Order.java`,`Product.java` and `User.java`: These files contain definitions for all vertices (csv files). 
2. `Link.java`: Contains generic definition for edges between vertices (like for order_product.csv file).
3. `ProdMeta.java` and `UserPro.java`: Contains definitions for new collections (prod_merge and user_pro).
4. `Arango.java`: It has basic ArangoDB functions used for export results to ArangoDB graph.
5. `SparkML.java` and `Test.java`: The training and testing procedure for individual methods are written in these files. 
6. `App.java`: This contains the main function and takes four integer arguments: 
	* `total`: Number of recommendations required.
	* `user_id`: target user.
	* `order_dow`: day of the week (0-6).
	* `order_hour_of_day`: Hour of the day (0-23).
7. Maven (only important ones):
	* `maven-compiler-plugin`: source = 1.8, target = 1.8
	* `spark-core`: 2.3.1
	* `spark-mllib`: 2.3.1
	* `spark-sql`: 2.3.1
	* `arangodb-java-driver`: 4.6.0
	* `arangodb-spark-connector`: 1.0.2


#### Excecution:
(Assuming ArangoDB and Spark are already installed and running.) 
1. Clone/Download the repository.
2. Build the jar file with all dependencies using this command:
	`mvn clean compile assembly:single`
3. To run the `*.jar`file in Apark Environment:
	`spark-submit --class com.arango.spark.instacart.App "/path/to/this/repo/target/*.jar" total user_id order_dow order_hour_of_day`

#### Example:
Suppose we run the above command with arguments:

	* total: 30
	* user_id: 24
	* order_dow: 1
	* order_hour_of_day: 21

We get the final output as:

	Tested for - user: 24 - DOW: 1.0 - Hour: 21.0

	Previously ordered (if any): 

	product_id - department - aisle - product_name

	31222 - beverages - juice nectars - 100% Juice, Variety Pack
	.
	.
	.


	Similar orders (sort-by rec_percent): 

	rec_percent - order_id - user_id - order_dow - order_hour_of_day order_number - day_since_prior_order

	92% - 710028 - 109988 - 4.0 - 15.0 - 53.0 - 7.0
	84% - 3152952 - 24723 - 0.0 - 14.0 - 52.0 - 8.0
	80% - 1258911 - 154366 - 1.0 - 21.0 - 14.0 - 30.0
	.
	.
	.
	46% - 2621617 - 122044 - 0.0 - 19.0 - 5.0 - 2.0
	23% - 1998525 - 171171 - 2.0 - 17.0 - 11.0 - 30.0
	19% - 965160 - 24 - 0.0 - 16.0 - 19.0 - 0.0


	ALS recommended products (sort-by rec_percent):

	rec_percent - product_id - department - aisle - product_name

	99% - 27845 - dairy eggs - milk - Organic Whole Milk
	86% - 196 - beverages - soft drinks - Soda
	85% - 522 - alcohol - spirits - Coffee Liqueur
	.
	.
	.
	8% - 18465 - dairy eggs - eggs - Organic Grade A Free Range Large Brown Eggs
	8% - 5876 - produce - fresh fruits - Organic Lemon
	8% - 20794 - beverages - refrigerated - Kombucha, Organic Raw, Citrus


	TFIDF Recommended products (sort-by department_id):

	rec_percent - product_id - department - aisle - product_name

	64% - 13870 - snacks - chips pretzels - Lightly Salted Baked Snap Pea Crisps
	96% - 514 - household - food storage - Freezer Safe Pint Jars Wide Mouth
	93% - 24838 - dairy eggs - soy lactosefree - Unsweetened Almondmilk
	.
	.
	.
	3% - 196 - beverages - soft drinks - Soda
	90% - 522 - alcohol - spirits - Coffee Liqueur
	87% - 24852 - produce - fresh fruits - Banana

