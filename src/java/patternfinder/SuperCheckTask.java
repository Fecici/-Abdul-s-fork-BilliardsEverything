package patternfinder;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.collections.api.list.primitive.ImmutableIntList;

import billiards.viewer.Utils;
import billiards.viewer.Viewer;
import billiards.wrapper.ConnectionPool;
import javafx.concurrent.Task;
import javaslang.collection.Array;

public final class SuperCheckTask extends Task<String>{

	private final int checkLength;

	final Array<Callable<String>> tasks;
	final ConnectionPool pool;

	public SuperCheckTask(final String inputText, final boolean lr, final int len, final ConnectionPool pool) {
	    checkLength = len;
		final String[] blocks = inputText.trim().split("\\r?\\n\\r?\\n");
		final Array<String> blockArray = Array.of(blocks);
	    this.pool = pool;
		this.tasks = blockArray.map(block -> () -> {
			if (block.contains("empty")) {
				return "";
			}
			if (block.contains("error")) {
				// to skip the error string at the top
				return "";
			}

			final String[] text = block.split("\\r?\\n");
			final String patStr;
			final String codeStr;
			if (text[0].contains("RESULTS")) {
				patStr = text[1].replace("// pat: ", "");
				codeStr = text[2].replace("// base: ", "");
			} else {
				patStr = text[0].replace("// pat: ", "");
				codeStr = text[1].replace("// base: ", "");
			}
			if (patStr.contains(",")) {
				//triple
				for (int i = 0; i < 3; i++) {
					final Optional<ImmutableIntList> pat = Utils.splitString(patStr.split(",")[i]);
					final Optional<ImmutableIntList> code = Utils.splitString(codeStr.split(",")[i]);
	    			if (code.isPresent() && pat.isPresent()) {
                        final ImmutableIntList newCode = PatUtils.addImm(code.get(), pat.get(), checkLength);

                        final boolean full;

                        System.out.println("Checking " + newCode);

	    			    if (lr) full = PatUtils.emptyVerifyLR(code.get(), newCode, pool);
	    			    else full = PatUtils.emptyVerify(newCode, pool);

	    			    if (!full) return "removePls" + block;
	    			}
				}
			} else {
				//single
				final Optional<ImmutableIntList> pat = Utils.splitString(patStr);
				final Optional<ImmutableIntList> code = Utils.splitString(codeStr);
    			if (code.isPresent() && pat.isPresent()) {
    				final ImmutableIntList newCode = PatUtils.addImm(code.get(), pat.get(), checkLength);

    				final boolean full;

                    System.out.println("Checking " + newCode);

                    if (lr) full = PatUtils.emptyVerifyLR(code.get(), newCode, pool);
                    else full = PatUtils.emptyVerify(newCode, pool);

                    if (!full) return "removePls" + block;
    			}
			}
			return block;
		});
	}

	@Override
	protected String call(){
		final StringBuilder result = new StringBuilder();
		final StringBuilder antiResult = new StringBuilder();

        final ExecutorService executor = Executors.newFixedThreadPool(Utils.numThreads);
        final Array<Future<String>> futures =
        			this.tasks.map(task -> executor.submit(task));
        Optional<ExecutionException> except = Optional.empty();

        int progress = 0;
        final int todo = futures.size();

        for (final Future<String> future : futures) {
        	if (this.isCancelled() || except.isPresent()) {
                future.cancel(false);
            } else {
            	try {
                    final String testedBlock = future.get();

                    if (!testedBlock.startsWith("removePls")) result.append(testedBlock + "\n\n");

                    else antiResult.append(testedBlock.replace("removePls", "")+ "\n\n");

                    progress += 1;
                    this.updateProgress(progress, todo);

            	} catch (final ExecutionException e) {
                    except = Optional.of(e);

                } catch (final InterruptedException e) {
                    if (!this.isCancelled()) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        executor.shutdown();

        Utils.writeToFile(Viewer.tmpDir + "/superFails.txt", antiResult.toString());

		return result.toString();
	}

}
