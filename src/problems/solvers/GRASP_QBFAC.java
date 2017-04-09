package problems.solvers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

import metaheuristics.grasp.AbstractGRASP;
import metaheuristics.grasp.Pair;
import problems.qbfac.QBFAC_Inverse;
import solutions.Solution;

/**
 * Metaheuristic GRASP (Greedy Randomized Adaptive Search Procedure) for
 * obtaining an optimal solution to a QBF (Quadractive Binary Function --
 * {@link #QuadracticBinaryFunction}) with adjacency constraints. 
 * Since by default this GRASP considers minimization problems, 
 * an inverse QBF function is adopted.
 * 
 * @author ccavellucci, fusberti
 */
public class GRASP_QBFAC extends AbstractGRASP<Integer> {

	private long frequency[];
	
	private boolean perturb;
	
	private static Double flagCost = null;
	
	private static long startTime;
	private static long endTime;
	
	/**
	 * Constructor for the GRASP_QBFAC class. An inverse QBF with adjacency constraints
	 * objective function is passed as argument for the superclass constructor.
	 * 
	 * @param alpha
	 *            The GRASP greediness-randomness parameter (within the range
	 *            [0,1])
	 * @param iterations
	 *            The number of iterations which the GRASP will be executed.
	 * @param filename
	 *            Name of the file for which the objective function parameters
	 *            should be read.
	 * @throws IOException
	 *             necessary for I/O operations.
	 */
	public GRASP_QBFAC(Double alpha, boolean bestimproving, boolean perturb, String filename) throws IOException {
		super(new QBFAC_Inverse(filename), alpha, bestimproving);
		this.perturb = perturb;
		this.frequency = new long[ObjFunction.getDomainSize()];
	}

	/* (non-Javadoc)
	 * 
	 * @see grasp.abstracts.AbstractGRASP#makeCL()
	 */
	@Override
	public ArrayList<Integer> makeCL() {
		
		ArrayList<Integer> _CL = new ArrayList<Integer>();
		for (int i = 0; i < ObjFunction.getDomainSize(); i++) {
			Integer cand = new Integer(i);
			_CL.add(cand);
		}

		return _CL;
	}

	/* (non-Javadoc)
	 * 
	 * @see grasp.abstracts.AbstractGRASP#makeRCL()
	 */
	@Override
	public ArrayList<Integer> makeRCL() {

		ArrayList<Integer> _RCL = new ArrayList<Integer>();

		return _RCL;
	}

	/* (non-Javadoc)
	 * 
	 * @see grasp.abstracts.AbstractGRASP#updateCL()
	 */
	@Override
	public void updateCL() {
		
        // Adicionando a restrição de adjacência
		/*if (!incumbentSol.isEmpty()) {
			Integer lastIn = incumbentSol.get(incumbentSol.size() - 1);
			CL.remove(new Integer(lastIn-1));
			CL.remove(new Integer(lastIn+1));
		}*/
		
		CL = new ArrayList<Integer>();
		for (int i = 0; i < ObjFunction.getDomainSize(); i++) {
			Integer left = new Integer(i-1);
			Integer cand = new Integer(i);
			Integer right = new Integer(i+1);
			if (!incumbentSol.contains(left) && !incumbentSol.contains(cand) 
				&& !incumbentSol.contains(right)) {
				CL.add(cand);
			}	
		}
		
	}

	/**
	 * {@inheritDoc}
	 * 
	 * This createEmptySol instantiates an empty solution and it attributes a
	 * zero cost, since it is known that a QBF solution with all variables set
	 * to zero has also zero cost.
	 */
	@Override
	public Solution<Integer> createEmptySol() {
		Solution<Integer> sol = new Solution<Integer>();
		sol.cost = 0.0;
		return sol;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * The local search operator developed for the QBF objective function is
	 * composed by the neighborhood moves Insertion, Removal and 2-Exchange.
	 */
	@Override
	public Solution<Integer> localSearch() {

		Double minDeltaCost;
		Integer bestCandIn = null, bestCandOut = null;

		do {
			
			minDeltaCost = Double.POSITIVE_INFINITY;
			
			updateCL();
			
			// Evaluate insertions
			for (Integer candIn : CL) {
				double deltaCost = ObjFunction.evaluateInsertionCost(candIn, incumbentSol);
				if (deltaCost < minDeltaCost) {
					minDeltaCost = deltaCost;
					bestCandIn = candIn;
					bestCandOut = null;
				}
			}
			
			// Evaluate removals
			for (Integer candOut : incumbentSol) {
				double deltaCost = ObjFunction.evaluateRemovalCost(candOut, incumbentSol);
				if (deltaCost < minDeltaCost) {
					minDeltaCost = deltaCost;
					bestCandIn = null;
					bestCandOut = candOut;
				}
			}
			
			// Evaluate exchanges
			for (Integer candOut : incumbentSol) {

				updateCL();
				
				for (Integer candIn : CL) {
					double deltaCost = ObjFunction.evaluateExchangeCost(candIn, candOut, incumbentSol);
					if (deltaCost < minDeltaCost) {
						minDeltaCost = deltaCost;
						bestCandIn = candIn;
						bestCandOut = candOut;
					}
				}
			}
			
			// Implement the best move, if it reduces the solution cost
			if (minDeltaCost < -Double.MIN_VALUE) {
				
				if (bestCandIn != null) {
					incumbentSol.add(bestCandIn);
					CL.remove(bestCandIn);
				}

				if (bestCandOut != null) {
					incumbentSol.remove(bestCandOut);				
					CL.add(bestCandOut);
				}

				ObjFunction.evaluate(incumbentSol);
			}
			
		} while (bestimproving && minDeltaCost < -Double.MIN_VALUE);

		for (Integer i : incumbentSol) {
			frequency[i]++;
		}

		return null;
	}
	
	@Override
	public Double perturbation(Integer c, long iteration) {
		
		if (iteration < 3 || !perturb) { // é para perturbar?? 
			return 1.0;
		}
			
		if (iteration % 3 == 1){
			return (1.25 + 0.75 * frequency[c] / iteration);
		} 
		
		if (iteration % 3 == 0){
			return (2 - 0.75 * frequency[c] / iteration);
		}

		return 2.0;
	}
	
	@Override
	public boolean solveStopCriteria(Double bestCost) {
		
		endTime = System.currentTimeMillis();
		
		if (flagCost != null && flagCost.compareTo(bestCost) == 0) {
			return true;
		}
		
		return ((endTime - startTime) >= 1800000); //30*60*1000 30min
	}

	/**
	 * A main method used for testing the GRASP metaheuristic.
	 */
	public static void main(String[] args) throws IOException {

		String[] filename = {"qbf020", "qbf040", "qbf060", "qbf080", "qbf100"};
		Double[] baseCost = {-104.0, -251.0, null, null, null};
		
		String[] Search = {"First-improving", "Best-improving"};
				
		boolean[] flag = {true, false};
		
		Double[] alpha = {0.2, 0.8};

		for (int index = 0; index < 5; ++index) { // alterna as instâncias 
			
			String path = "instances/"+filename[index];
			
			flagCost = baseCost[index];
			
			for (int i = 0; i < 8; ++i) {
				
				System.out.print("Instance = "+filename[index]+", ");
				System.out.print("Alpha = "+alpha[i/4]+", ");
				System.out.print("Cost Perturbation = "+flag[i%2]+", ");
				System.out.print(Search[(i%4)/2]+", ");
				
				startTime = System.currentTimeMillis();
				GRASP_QBFAC grasp = new GRASP_QBFAC(alpha[i/4], flag[(i%4)/2], flag[i%2], path);
				Pair<Solution<Integer>, Solution<Integer>> bestSol = grasp.solve();
				
				Comparator<Integer> cmp = new Comparator<Integer>() {
			        @Override
			        public int compare(Integer first, Integer second) {
			            return first.compareTo(second);
			        }
				};
				
				bestSol.getL().sort(cmp);
				bestSol.getR().sort(cmp);
				
				long totalTime = endTime - startTime;
				double seg = (double)totalTime/(double)1000;
				
				System.out.println("Time = "+seg+" seg");
				System.out.println("MaxBCH = " + bestSol.getR());
				System.out.println("MaxVal = " + bestSol.getL()+"\n");
			}
		}
		
		//System.out.print("It's finished!!!");
	}

}

