package mikera.tyrant.perf;

import mikera.tyrant.engine.Lib;

public class Lib_Perf extends Perf {
    public Lib_Perf() {
    	//empty
    }

    public static void main(String[] args) {
        new Lib_Perf().go(args);
    }

    public void go(String[] args) {
        if(args.length != 2) {
            printUsage();
            return;
        }
        if(args == null || args.length == 0) args = new String[] {KillAllBaddies.class.getName(), "10"};
        IWork work = findWork(args[0]);
        int iterations = Integer.parseInt(args[1]);
        WorkDone[] workFinished = timeToRun(work, iterations);
        oneLineSummary(shortName(work.getClass()), work.getMessage(), workFinished);
    }

    private String shortName(Class aClass) {
        String name = aClass.getName();
        int lastDot = name.lastIndexOf('.');
        return name.substring(lastDot + 1);
    }

    private void printUsage() {
        System.out.println("Usage java " + Lib_Perf.class.getName() + " 'name of work' iterations");
        System.out.println("Example: " + Lib_Perf.class.getName() + " " + KillAllBaddies.class.getName() + " 10");
        System.out.println("This will execute KillAllBaddies 10 times.");
    }

    private IWork findWork(String className) {
        try {
            return (IWork) Class.forName(className).newInstance();
        } catch(ClassNotFoundException cnfe) {
            Class theClass = KillAllBaddies.class;
            System.out.println("Class " + className + " not found, using " + theClass.getName() + " instead.");
            try {
                return (IWork) theClass.newInstance();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    protected Runnable createLib() {
        Runnable work = new Runnable() {
            public void run() {
                Lib.clear();
                Lib.instance();
            }
        };
        return work;
    }
}
