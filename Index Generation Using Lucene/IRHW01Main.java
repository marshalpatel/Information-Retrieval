package ir.IRHW01.com;

import java.io.IOException;

public class IRHW01Main {

	public static void main(String[] args) throws IOException{
		// TODO Auto-generated method stub

		 generateIndex std = new generateIndex();
		 std.createIndex("E:\\Marshal\\Masters\\Search\\Assignment1\\corpus\\corpus");
		 
		indexComparison ic = new indexComparison();
		
		ic.compareIndex();

		 


	}

}
