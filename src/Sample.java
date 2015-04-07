import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Sample {
    public static void main(String[] args) {
      BufferedReader in;
      int[] ctext = new int[48];
      Oracle oracle;
      int rc, i = 0;

      if(args.length < 1) {
        System.out.println("Usage: java Test <filename>");
        System.exit(-1);
      }

      try {
        in = new BufferedReader(new FileReader(args[0]));

        char[] buf = new char[2];
        while( in.read(buf,0,2) != -1) {
          ctext[i++] = (Integer.parseInt(new String(buf),16));
        }

        oracle = new Oracle();

        oracle.connect();

        rc = oracle.send(ctext,3);
        System.out.printf("Oracle returned: %d\n", rc);
        
//        for (int j=0; j<ctext.length; j++) {
//        	ctext[j] = 0x00;
//        	rc = oracle.send(ctext,3);
//        	System.out.printf("Oracle returned: %d, for first %d bytes set as 0x00.\n", rc, j);
//        	if (rc == 0)
//        		break;
//        }
        // When first 22 bytes set as 0x00, Oracle returned 0.
        // Therefore, padding is 0x0B, plain text length is 20 bytes.
        
        // start to crack plain text
        int[] ptext = new int[21];
        
        // crack the (j+1)th byte, j = 16 to 20
        for (int j=20; j>=16; j--) {
        	// for the last 32-j bytes in 2nd block of cipher,
        	// C xor 0x0B (correct padding) = C' xor (32-j).
        	int[] ctext_copy = ctext;
        	for (int k=j+1; k<32; k++) {
        		ctext_copy[k] = (ctext[k] ^ (32-21)) ^ (32-j);
        	}
        	// then for the jth byte, test 0x00 to 0xff to see which value decrypts
        	for (int k=0; k<256; k++) {
        		ctext_copy[j] = k;
        		rc = oracle.send(ctext_copy,3);
        		System.out.printf("Oracle returned: %d, for %dth bytes set as 0x%02x.\n", rc, j, k);
        		if (rc == 1) {
        			// c' xor padding = c xor m
        			// m = c' xor padding xor c
        			ptext[j] = k ^ (32-j) ^ ctext[j];
        			break;
        		}
        	}
        }
        

        oracle.disconnect();

      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
}
