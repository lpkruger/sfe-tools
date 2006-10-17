package sfe.editdist;

import java.io.IOException;

public class EDProto2 extends EDProto {
	static class Alice extends EDProto.Alice {
		Alice(String to, int port, String str1) throws IOException {
			super(to, port, str1);
		}
		
		void computeRecurrence(int i, int j) throws Exception {
			
		}
	}

	static class Bob extends EDProto.Bob {
		Bob(int port, String str2) throws IOException {
			super(port, str2);
		}
		
		void computeRecurrence(int i, int j) throws Exception {
		
		}
	}
}
