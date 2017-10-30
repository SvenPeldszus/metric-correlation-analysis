package metricTool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class Download {
	

	public void organizeDownloads(List<String> downloadURLs){
		for(String url : downloadURLs){
			if(!gitClone(url))
				System.err.println(url + ": not successfully cloned");
		}
	}
	
	public boolean gitClone(String url){
		String cmd = "cd " + Executor.result_dir + "VersionTemp\\Sourcecode" + " && " + "git clone --recursive " + url;
		System.out.println(cmd);
		Runtime run = Runtime.getRuntime();
		try {
			Process process;
			if(Executor.windows){
				System.out.println("windows");
				process = run.exec("cmd /c \"" + cmd );
				// + " && exit\""
				BufferedReader stream_reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line;
				while ((line = stream_reader.readLine()) != null) {
					System.out.println("> " + line); //$NON-NLS-1$
				}
				process.waitFor();
				process.destroy();
				stream_reader.close();
				return true;
			}
			else if(Executor.linux){
				process = run.exec(cmd);
				return false;
			}
			else {
				System.err.println("Program is not compatibel with the Operating System");
				return false;
			}
			
			
			
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean changeVersion(File src_code, File version_ids){
		Executor executor = new Executor();
		File result_file = new File(Executor.result_dir + "VersionTemp\\results\\VersionResult.csv");
		try {
			String[] ids = new String[3];
			BufferedReader reader = new BufferedReader(new FileReader(version_ids));
			String line = reader.readLine();
			System.out.println(line);
			int index = 0;
			while(line != null){
				ids[index] = line;
				line = reader.readLine();
				index++;
			}			
			reader.close();
			File build = new File(src_code, "build");
			if(build.exists())
				Executor.clear(build);
			int count = 1;
			for(String id : ids){
				System.out.println(id);
				if(build.exists())
					Executor.clear(build);
				String cmd = "cd " + src_code.getPath() + " && " + "git checkout " + id + " .";
				Runtime run = Runtime.getRuntime();
				try {
					Process process;
					if(Executor.windows){
						process = run.exec("cmd /c \"" + cmd + " && exit\"");
					}
					else if(Executor.linux){
						process = run.exec(cmd);
					}
					else {
						System.err.println("Program is not compatibel with the Operating System");
						return false;
					}
					
					BufferedReader stream_reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
					while ((line = stream_reader.readLine()) != null) {
						System.out.println("> " + line); //$NON-NLS-1$
					}
					process.waitFor();
					process.destroy();
					stream_reader.close();

				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
				
				executor.mainProcess(src_code, count, result_file);
				count++;
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
			
		return false;
	}
}