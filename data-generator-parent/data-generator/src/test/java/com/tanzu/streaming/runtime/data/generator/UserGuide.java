package com.tanzu.streaming.runtime.data.generator;

import java.util.Random;

import com.tanzu.streaming.runtime.data.generator.context.SharedFieldValuesContext;

public class UserGuide {

	public static void main(String[] args) {
		new UserGuide().case0();
		new UserGuide().case1();
		new UserGuide().case2();
		new UserGuide().case3();
		new UserGuide().case4();
		new UserGuide().case5();
	}

	private void case0() {
		System.out.println("Case 0");
		DataUtil.print(new DataGenerator(
				DataUtil.uriToSchema("classpath:/avro/userguide/user0.yaml"), 3));

//		{"id":"xrabkxrkifohqgoemjgwlqxfxxtgpkj","sendAt":-807979869550693825,"fullName":"lbpagnstqgu","email":"ito","age":962901484}
//		{"id":"qo","sendAt":-8783467254640473491,"fullName":"yosqlxvysblcyqquebuxwxxl","email":"hxesgbyprjchknfuplvwjcglfug","age":-594154468}
//		{"id":"uefcjprhhqkeoglqqds","sendAt":1063234558797032480,"fullName":"egsxhjdknihhavuf","email":"","age":1741049857}

// While those are valid records compliant with the avro schema they are hardly readable. We can do better by annotating the schema with some Data faker hits.

	}

	private void case1() {
		System.out.println("Case 1");

		DataUtil.print(new DataGenerator(
				DataUtil.uriToSchema("classpath:/avro/userguide/user1.yaml"), 3));

//		{"id":"133-07-1010","sendAt":1645204006702,"fullName":"Frances Legros","email":"sonja.marks@hotmail.com","age":79}
//		{"id":"828-63-1547","sendAt":1645204006791,"fullName":"Ms. Shenika Connelly","email":"jayson.smitham@yahoo.com","age":43}
//		{"id":"609-96-9798","sendAt":1645204006794,"fullName":"Candie Dooley","email":"jc.ernser@yahoo.com","age":27}

// This is much better. But note that the email name is unrelated with the full name!
// Can we make the email field reuse the fullName field when computing the email?
	}

	private void case2() {
		System.out.println("Case 2");
		DataUtil.print(new DataGenerator(
				DataUtil.uriToSchema("classpath:/avro/userguide/user2.yaml"), 3));

//		{"id":"459-62-6235","sendAt":1645204768085,"fullName":"Beckie Mills","email":"beckie.mills@yahoo.com","age":61}
//		{"id":"235-11-3358","sendAt":1645204768124,"fullName":"Ignacio Hartmann","email":"ignacio.hartmann@hotmail.com","age":44}
//		{"id":"512-66-8911","sendAt":1645204768129,"fullName":"Cesar McDermott","email":"cesar.mcdermott@hotmail.com","age":57}

// Great! now the email name corresponds to the user's full name.
// But if the user_ids are the same we would like the rest of the record to be the same too. It makes no sense to
// have many users with the same ID. Like this.

//		{"id":"100-00-0000","sendAt":1645205272568,"fullName":"Scot O'Reilly","email":"#{internet.emailAddress 'scot.o'reilly'}","age":23}
//		{"id":"100-00-0000","sendAt":1645205272578,"fullName":"Pat Effertz","email":"pat.effertz@yahoo.com","age":77}
//		{"id":"100-00-0000","sendAt":1645205272613,"fullName":"Mrs. Torri Predovic","email":"mrs..torri.predovic@yahoo.com","age":59}
//		{"id":"200-00-0000","sendAt":1645205272607,"fullName":"Kasey Wunsch","email":"kasey.wunsch@hotmail.com","age":8}
//		{"id":"200-00-0000","sendAt":1645205272610,"fullName":"Bo Brakus DVM","email":"bo.brakus.dvm@yahoo.com","age":41}
	}

	private void case3() {
		System.out.println("Case 3");
		// Let's add a key=id to the records' doc. This instructs the generator to retain only one record copy per `id`.
		DataUtil.print(new DataGenerator(
				DataUtil.uriToSchema("classpath:/avro/userguide/user3.yaml"), 5));

//		{"id":"100-00-0000","sendAt":1645226876018,"fullName":"Haley Parisian","email":"haley.parisian@hotmail.com","age":71}
//		{"id":"100-00-0000","sendAt":1645226876018,"fullName":"Haley Parisian","email":"haley.parisian@hotmail.com","age":71}
//		{"id":"200-00-0000","sendAt":1645226876035,"fullName":"Sammie Wyman","email":"sammie.wyman@gmail.com","age":27}
//		{"id":"200-00-0000","sendAt":1645226876035,"fullName":"Sammie Wyman","email":"sammie.wyman@gmail.com","age":27}
//		{"id":"200-00-0000","sendAt":1645226876035,"fullName":"Sammie Wyman","email":"sammie.wyman@gmail.com","age":27}

	}

	private void case4() {

// Let's introduce the Click stream data (click.yaml)  as well.
		System.out.println("Case 4");
		SharedFieldValuesContext sharedFieldValuesContext = new SharedFieldValuesContext(new Random());

		DataUtil.print(new DataGenerator(
				DataUtil.uriToSchema("classpath:/avro/userguide/user3.yaml"), 5,
				sharedFieldValuesContext));

		System.out.println("=========================================================================");

		DataUtil.print(new DataGenerator(
				DataUtil.uriToSchema("classpath:/avro/userguide/click.yaml"), 5,
				sharedFieldValuesContext));

//		{"id":"200-00-0000","sendAt":1645226876069,"fullName":"Rafael Renner","email":"rafael.renner@gmail.com","age":50}
//		{"id":"100-00-0000","sendAt":1645226876078,"fullName":"Verlene Orn","email":"verlene.orn@hotmail.com","age":14}
//		{"id":"200-00-0000","sendAt":1645226876069,"fullName":"Rafael Renner","email":"rafael.renner@gmail.com","age":50}
//		{"id":"100-00-0000","sendAt":1645226876078,"fullName":"Verlene Orn","email":"verlene.orn@hotmail.com","age":14}
//		{"id":"100-00-0000","sendAt":1645226876078,"fullName":"Verlene Orn","email":"verlene.orn@hotmail.com","age":14}
//		---
//		{"user_id":"766-48-6587","page":7932,"action":"products","user_agent":"Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_6; en-en) AppleWebKit/533.19.4 (KHTML, like Gecko) Version/5.0.3 Safari/533.19.4"}
//		{"user_id":"820-93-3557","page":4970,"action":"product_detail","user_agent":"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322)"}
//		{"user_id":"672-69-8730","page":60723,"action":"product_detail","user_agent":"Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0)"}
//		{"user_id":"741-66-2901","page":2291,"action":"checkout","user_agent":"Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.0.2) Gecko/20030208 Netscape/7.02"}
//		{"user_id":"514-91-4473","page":35713,"action":"cart","user_agent":"Mozilla/4.0 (compatible; MSIE 6.0; AOL 9.0; Windows NT 5.1; SV1; .NET CLR 1.0.3705; .NET CLR 1.1.4322; Media Center PC 4.0)"}

		// The User and the Click data looks presentable.
		// But although the clickstream is supposed to represent user's click history apparently none of the user's Ids match any Click#uer_id!
		// How can we make this more realistic and correlate the data sources on the user id?
	}

	private void case5() {
		System.out.println("Case 5");

		// The SharedFieldValuesContext help correlation
		try (SharedFieldValuesContext sharedFieldValuesContext = new SharedFieldValuesContext(new Random())) {

			DataUtil.print(new DataGenerator(
					DataUtil.uriToSchema("classpath:/avro/userguide/user4.yaml"), 3,
					sharedFieldValuesContext));

			System.out.println("---");

			DataUtil.print(new DataGenerator(
					DataUtil.uriToSchema("classpath:/avro/userguide/click1.yaml"), 3,
					sharedFieldValuesContext));
		}

//		{"id":"238-73-3972","sendAt":1645226876143,"fullName":"Kristle Stehr","email":"kristle.stehr@gmail.com","age":68}
//		{"id":"136-86-3142","sendAt":1645226876154,"fullName":"Dr. Galen Stokes","email":"dr..galen.stokes@yahoo.com","age":43}
//		{"id":"142-39-7287","sendAt":1645226876158,"fullName":"Jarod Predovic","email":"jarod.predovic@yahoo.com","age":10}
//		{"id":"158-04-2595","sendAt":1645226876164,"fullName":"Kirstie Gottlieb","email":"kirstie.gottlieb@gmail.com","age":37}
//		{"id":"448-56-8297","sendAt":1645226876169,"fullName":"Beatris Crooks","email":"beatris.crooks@yahoo.com","age":37}
//		---
//				{"user_id":"136-86-3142","page":55475,"action":"checkout","user_agent":"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)"}
//		{"user_id":"136-86-3142","page":46132,"action":"selection","user_agent":"Mozilla/4.0 (compatible; MSIE 6.0; AOL 9.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322)"}
//		{"user_id":"238-73-3972","page":30697,"action":"product_detail","user_agent":"Mozilla/4.0 (compatible; MSIE 9.0; Windows NT 6.1; 125LA; .NET CLR 2.0.50727; .NET CLR 3.0.04506.648; .NET CLR 3.5.21022)"}
//		{"user_id":"238-73-3972","page":19479,"action":"cart","user_agent":"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36 OPR/42.0.2393.94"}
//		{"user_id":"448-56-8297","page":29109,"action":"cart","user_agent":"Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko"}

	}

}
