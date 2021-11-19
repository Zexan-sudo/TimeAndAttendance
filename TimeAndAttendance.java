import java.util.ArrayList;
import java.util.Calendar;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.InputMismatchException;



/*
Author:
Noah Brugnoli
ISTM 325 - 500
4/15/2020

Author Notes:
$20 and hour minimum wage for employees
General Manager - 4 hrs sick and 8 hrs vaca for each week paid atleast 38 hours
Shift Manager - 2 hrs sick and 4 hrs vaca for each week worked 8 hours
Assistant Manager - 2 hrs sick and 3 hrs vaca for each week worked 5 hours
Employee - 2 hrs sick and 2 hrs vaca for each week worked 5 hours

employee can only work for one location 

vaca only good for 2 years before paid out to employee
sick only good for 1 year before disapears, not paid out

all employees and managers clock out
if forget to clock out then a manager must manually create a clock in or out record for them
no manager can create a clock in or out record for themselves
all timecards are approved by shift manager and general manager approves all shift manager and his/her own timecard
unapproved timecards will not be paid but will remain in a pending state.

Must output hours worked, hours of sick leave used, hours of vaca used, number of hours of sick,
vaca avaliable for each employee and generate paycheck
*/

//not required for command arguments but have to accept htem if there.


class BadArgumentsException extends Exception 
{
    public BadArgumentsException(String s)
    {
        super(s);
    }
}

class BadMenuChoiceException extends Exception 
{
    public BadMenuChoiceException(String s)
    {
        super(s);
    }
}

class UnknownPositionException extends Exception
{
    public UnknownPositionException(String s)
    {
        super(s);
    }
}

class DontHaveAccessException extends Exception
{
    public DontHaveAccessException(String s)
    {
        super(s);
    }
}

class PTORequestDoesNotExistException extends Exception
{
    public PTORequestDoesNotExistException(String s)
    {
        super(s);
    }
}

class BadPTORequestException extends Exception
{
    public BadPTORequestException(String s)
    {
        super(s);
    }
}


class TimeAndAttendance
{
    //create an allocatePTO method somewhere that occurs at the end of each week
    static ArrayList<Location> locations = new ArrayList<Location>(10);
    static Scanner scnr = new Scanner(System.in);
    static ArrayList<String[]> PTORequests = new ArrayList<String[]>(50);
    static ArrayList<String> missingPunches = new ArrayList<String>(50);
    static boolean access;
    static Employee currUser = null;
    static Location userLocation = null;
    static int currentWeekOfYear;
    public static void main(String[] args)
    {
        try
        {
            loadCommandLineFiles(args);
            boolean menuChoice;
            while(true)
            {
                getCurrentUser();
                userLocation = currUser.getHomeLocation();
                if(access)
                {
                    do
                    {
                        System.out.println();
                        menuChoice = supervisorMenu();
                    }
                    while(menuChoice);
                }
                else if(!access)
                {
                    do
                    {
                        System.out.println();
                        menuChoice = employeeMenu();
                    }
                    while(menuChoice);
                }
            }
        }
        catch(BadArgumentsException n)
        {
            System.out.println("BadArgumentsException: " + n.getMessage());
        }
        catch(Exception e)
        {
            System.out.println("Something went wrong. " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Asks for the current users ID and assigns the globals variables currUser and userLocation
     */
    public static void getCurrentUser()
    {
        while(true)
        {
            try
            {
                System.out.print("Enter your EmpID: ");
                long currUserID = Long.parseLong(scnr.nextLine());
                for(Location tempLocation : locations)
                {
                    currUser = tempLocation.searchEmployees(currUserID);
                    if(currUser == null) continue;
                    else break;
                }
                if(currUser == null) throw new NumberFormatException();
                if(currUser instanceof Supervisor)
                {
                    access = true;
                    break;
                }
                if(currUser instanceof Employee)
                {
                    access = false;
                    break;
                } 
                userLocation = currUser.getHomeLocation();
            }
            catch(NumberFormatException n)
            {
                System.out.println("EmpID entered does not exist.");
            }
        }
    } 

    /**
     * used for loading command line files. Check the header of each file to find out which file type it is
     * @param args
     * @throws BadArgumentsException
     */
    public static void loadCommandLineFiles(String[] args) throws BadArgumentsException
    {
        try
        {
            while(args.length < 1 || args.length > 4)
            {
                throw new BadArgumentsException("In order to use this program you must: \n"
                                                    + "1. Enter the name of an employee csv file.\n"
                                                    + "2. (optional) Enter the name of a punch csv file. \n"
                                                    + "3. (optional) Enter the name of a time off csv file.\n"
                                                    + "4. (optional) Enter the name of a PTO request csv file.\n");
            }
            for(int i = 0; i<args.length; ++i)
            {
                Scanner filescanner = new Scanner(new File(args[i])); //will throw exception if file not found
                String line = filescanner.nextLine(); //read in the line
                String[] values = line.split(","); //make an array of the strings, split on the comma
                String fileHeader = String.join(",", values);
                if(i == 0 && !(fileHeader.equals("EmpID,Fname,Lname,Position,Location"))) 
                {
                    filescanner.close();
                    throw new BadArgumentsException("First file imported must be an employee data file.\n");
                }
                else if(fileHeader.equals("EmpID,Fname,Lname,Position,Location")) loadEmployees(args[i]);
                else if(fileHeader.equals("EmpID,Date,time,inOrOut")) loadPunchTimes(args[i]);
                else if(fileHeader.equals("EmpID,Date Requested Off,Hours Requested Off,Type (V/S),Status")) loadPTORequests(args[i]);
                else if(fileHeader.equals("EmpID,Type,Amount,Expiration Date")) loadTimeOff(args[i]);
                else
                {
                    filescanner.close();
                    throw new FileNotFoundException("Headers on " + args[i] + " does not match any file type.\n"
                                                + "For Employee data files: EmpID,Fname,Lname,Position,Location\n"
                                                + "For Punch data files: EmpID,Date,time,inOrOut\n"
                                                + "For Time off data files: EmpID,Date Requested Off,Hours Requested Off,Type (V/S),Status\n"
                                                + "For PTO requests data files: EmpID,Type,Amount,Expiration Date\n");
                }
                filescanner.close();
            }
        }
        catch(ParseException p)
        {
            System.out.println("ParseException: " + p.getMessage());
        }
        catch(FileNotFoundException f)
        {
            System.out.println("FileNotFoundException: " + f.getMessage());
        }
        catch(NumberFormatException n)
        {
            System.out.println("NumberFormatException: " + n.getMessage());
        }
    }

    /**
     * menu shown to supervisors, gives added features
     * @return
     * @throws Exception
     */
    public static boolean supervisorMenu() throws Exception
    {
        //when generating paycheck you have to check for expried PTO.
        int pendingCounter = 0;
        for(String[] s:PTORequests) 
        {
            if(userLocation.searchEmployees(Long.parseLong(s[0])) != null) ++pendingCounter;
        }
        System.out.println("There is " + pendingCounter + " PTO request(s) pending for approval.");
        String tempMenuChoice;
        boolean menuChoice = true; 
        System.out.println("1. Clock In"); //done
        System.out.println("2. Clock Out"); //done
        System.out.println("3. View PTO"); //done
        System.out.println("4. Request PTO"); //done
        System.out.println("5. Add/Change a Punch Time");//done
        System.out.println("6. View PTO Requests");//done
        System.out.println("7. View Employee PTO"); //To see other employees PTO. done
        System.out.println("8. Approve Timecards");//generates PTO
        System.out.println("9. Run Payroll");//done
        System.out.println("10. Export Data");
        System.out.println("11. Import Data"); //done
        System.out.println("12. View Employee Information"); //done
        System.out.println("0. Logout");//done
        System.out.print("Enter a menu option: ");//done
        tempMenuChoice = scnr.nextLine();
        tempMenuChoice.trim();
        long tempClockTime = 0;
        long tempHoursForClock = 0; //get date and hours from clock in and out
        long tempDateForClock = 0;
        boolean searchMenuChoice;
        DateFormat formatForDate = new SimpleDateFormat("MM/dd/yyyy");
        DateFormat formatForPayroll = new SimpleDateFormat("MM-dd-yyyy");
        DateFormat formatForHours = new SimpleDateFormat("HH:mm");
        DateFormat formatForPTOHour = new SimpleDateFormat("HH");
        Date dateCreator = new Date();
        String stringOfDate = new String();
        String stringOfHours = new String();
        try
        {
            if(tempMenuChoice.equals("1")) //clock in
            {
                tempClockTime = System.currentTimeMillis();

                dateCreator = new Date(tempClockTime);
                stringOfDate = formatForDate.format(dateCreator);
                dateCreator= formatForDate.parse(stringOfDate);
                tempDateForClock  = dateCreator.getTime();

                dateCreator = new Date(tempClockTime);
                stringOfHours = formatForHours.format(dateCreator);
                dateCreator = formatForHours.parse(stringOfHours);
                tempHoursForClock = dateCreator.getTime();

                Punch tempPunch = currUser.getLastPunch();
                if(tempPunch == null)
                {
                    Punch newPunch = new Punch(tempDateForClock, true, tempHoursForClock);
                    currUser.addPunch(newPunch);
                    System.out.println("You have been clocked in at " + newPunch.toString());
                }
                else if(tempPunch.getDate() == tempDateForClock)
                {
                    System.out.println("You already have a clock for today");
                } 
                else
                {
                    Punch newPunch = new Punch(tempDateForClock, true, tempHoursForClock);
                    currUser.addPunch(newPunch);
                    System.out.println("You have been clocked in at " + newPunch.toString());
                }
            }
            else if(tempMenuChoice.equals("2")) //clock out
            {
                tempClockTime = System.currentTimeMillis();

                dateCreator = new Date(tempClockTime);
                stringOfDate = formatForDate.format(dateCreator);
                dateCreator= formatForDate.parse(stringOfDate);
                tempDateForClock  = dateCreator.getTime();

                dateCreator = new Date(tempClockTime);
                stringOfHours = formatForHours.format(dateCreator);
                dateCreator = formatForHours.parse(stringOfHours);
                tempHoursForClock = dateCreator.getTime();
                
                Punch tempPunch = currUser.getLastPunch();
                if(tempPunch == null)
                {
                    Punch newPunch = new Punch(tempDateForClock, false, tempHoursForClock);
                    currUser.addPunch(newPunch);
                    System.out.println("You have been clocked out at " + newPunch.toString());
                }
                else if(tempPunch.getDate() == tempDateForClock && tempPunch.getOutTime() == 0)
                {
                    tempPunch.setOutTime(tempHoursForClock);
                    System.out.println("You have been clocked out at " + tempPunch.toString());
                }
                else if(tempPunch.getDate() == tempDateForClock && tempPunch.getOutTime() != 0)
                {
                    tempPunch.setOutTime(tempHoursForClock);
                    System.out.println("You already have a clock for today");
                }
                else
                {
                    Punch newPunch = new Punch(tempDateForClock, false, tempHoursForClock);
                    currUser.addPunch(newPunch);
                    System.out.println("You have been clocked out at " + newPunch.toString());
                }  
                
            }
            else if(tempMenuChoice.equals("3")) //view pto
            {
                do
                {
                    searchMenuChoice = viewPTO();
                }
                while(searchMenuChoice);
            }
            else if(tempMenuChoice.equals("4"))//request pto
            {
                int PTOHours;
                String PTODate;
                String PTOType;
                String newPTORequst;
                System.out.print("Enter the date you would like to use PTO (MM/dd/yyyy): ");
                PTODate = scnr.nextLine().trim();
                dateCreator = formatForDate.parse(PTODate);
                System.out.print("Enter the amount of hours you would like to use on " + PTODate + ": ");
                PTOHours = Integer.parseInt(scnr.nextLine().trim());
                if(PTOHours > 8) throw new BadPTORequestException("Cannot request more than 8 hours of PTO");
                System.out.print("What type of PTO would you like to use? (V/S): ");
                PTOType = scnr.nextLine().trim();
                if(PTOType.toLowerCase().equals("v"))
                {
                    newPTORequst = new String(currUser.getEmployeeID() + "," + PTODate + "," + PTOHours + "," + PTOType + ",Pending");
                    PTORequests.add(newPTORequst.split(","));
                }
                else if(PTOType.toLowerCase().equals("s"))
                {
                    newPTORequst = new String(currUser.getEmployeeID() + "," + PTODate + "," + PTOHours + "," + PTOType + ",Pending");
                    PTORequests.add(newPTORequst.split(","));
                }
                else
                {
                    throw new BadPTORequestException("Input was not of type Vacation (V) or Sick (S).");
                }
                System.out.println("PTO Request submitted.");
            }
            else if(tempMenuChoice.equals("5")) //add change punch time
            {
                do
                {
                    searchMenuChoice = addChangePunch();
                }
                while(searchMenuChoice);
            }
            else if(tempMenuChoice.equals("6"))//view pto request
            {
                do
                {
                    searchMenuChoice = viewPTORequests();
                }
                while(searchMenuChoice);
            }
            else if(tempMenuChoice.equals("7"))// view employee pto
            {
                do
                {
                    searchMenuChoice = viewEmployeePTO();
                }
                while(searchMenuChoice);
            }
            else if(tempMenuChoice.equals("8"))//Approve Timecards
            {
                String userChoice = new String();
                boolean input = false;
                String beginningOfWeekString = new String();
                String endOfWeekString = new String();
                long beginningOfWeekMilli = 0;
                long endOfWeekMilli = 0;
                boolean punchMissingShiftManager = false;
                boolean punchMissingGeneralManager = false;
                System.out.print("Enter the beginnning day of the week (MM/dd/yyyy): ");
                beginningOfWeekString = scnr.nextLine();
                System.out.print("Enter the ending day of the week (MM/dd/yyyy): ");
                endOfWeekString = scnr.nextLine();
                dateCreator = formatForDate.parse(beginningOfWeekString);
                beginningOfWeekMilli = dateCreator.getTime();
                dateCreator = formatForDate.parse(endOfWeekString);
                endOfWeekMilli = dateCreator.getTime();
                if(currUser.getPosition().equals("Shift Manager"))
                { 
                    userLocation.printShiftManagerTimeCards(beginningOfWeekMilli, endOfWeekMilli);
                    while(!input)
                    {
                        punchMissingShiftManager = userLocation.checkShiftManagerPunches();
                        if(punchMissingShiftManager == true)
                        {
                            System.out.println("\nAN EMPLOYEE IS MISSING PUNCHES FOR THE WEEK. CHECK TIME CARDS.\n");
                            userLocation.resetShiftManagerTimeCards();
                            input = true;
                        }
                        else
                        {
                            System.out.print("Would you like to approve these time cards? (Y/N): ");
                            userChoice = scnr.nextLine();
                            if(userChoice.toLowerCase().equals("y"))
                            {
                                userLocation.approveShiftManagerTimeCards();
                                System.out.println("Time cards Approved.\n");
                                input = true;
                            }
                            else if(userChoice.toLowerCase().equals("n")) input = true;
                            else System.out.println("Input Not Valid. Please Try Again.");
                        }
                    }
                }
                else if(currUser.getPosition().equals("General Manager")) 
                {
                    userLocation.printGeneralManagerTimeCards(beginningOfWeekMilli, endOfWeekMilli);
                    while(!input)
                    {
                        punchMissingGeneralManager = userLocation.checkGeneralManagerPunches();
                        if(punchMissingGeneralManager == true)
                        {
                            System.out.println("\nAN EMPLOYEE IS MISSING PUNCHES FOR THE WEEK. CHECK TIME CARDS.\n");
                            userLocation.resetGeneralManagerTimeCards();
                            input = true;
                        }
                        else
                        {
                            System.out.print("Would you like to approve these time cards? (Y/N): ");
                            userChoice = scnr.nextLine();
                            if(userChoice.toLowerCase().equals("y"))
                            {
                                userLocation.approveGeneralManagerTimeCards();
                                System.out.println("Time cards Approved.\n");
                                input = true;
                            }
                            else if(userChoice.toLowerCase().equals("n")) input = true;
                            else System.out.println("Input Not Valid. Please Try Again.");
                        }
                    }
                }
            }
            else if(tempMenuChoice.equals("9"))//run payroll
            {
                if(currUser.getPosition().equals("General Manager"))
                {
                    System.out.print("Enter the ending day of the week (MM-dd-yyyy): ");
                    String endOfWeekString = scnr.nextLine();
                    dateCreator = formatForPayroll.parse(endOfWeekString);
                    long endOfWeekMilli = dateCreator.getTime();
                    String payrollOutput = userLocation.runPayroll(endOfWeekMilli);
                    String filename = endOfWeekString + "Payroll.csv";
                    PrintWriter out = new PrintWriter(new File(filename));
                    out.println(payrollOutput);
                    System.out.println("Payroll for the week of " + endOfWeekString + " exported to " + filename);
                    out.close();
                }
                else System.out.println("Only General Managers can run payroll");
            }
            else if(tempMenuChoice.equals("10"))//export data
            {
                exportEmployeeData();
                exportTimeOffData();
                exportPTORequests();
                exportPunches();
            }
            else if(tempMenuChoice.equals("11"))//import data
            {
                do
                {
                    searchMenuChoice = importMenu();
                }
                while(searchMenuChoice);
            }
            else if(tempMenuChoice.equals("12"))
            {
                long tempID;
                Employee tempEmployee = null;
                System.out.print("Enter the Employee ID for the employee you want to view: ");
                tempID = Long.parseLong(scnr.nextLine().trim());
                for(Location l:locations)
                {
                    tempEmployee = l.searchEmployees(tempID);
                    if(tempEmployee == null) continue;
                    else break;
                }
                if(tempEmployee == null) throw new NumberFormatException();
                System.out.println("\n" + tempEmployee.toString());
            }
            else if(tempMenuChoice.equals("0"))
            {
                menuChoice = false;
            }
            else
            {
                throw new BadMenuChoiceException("Input was not a menu option.");
            }
        }
        catch(NumberFormatException n)
        {
            System.out.println("Employee does not exist or hours entered was not a whole hour.");
        }
        catch(BadPTORequestException b)
        {
            System.out.println("\nBadPTORequestException: " + b.getMessage());
        }
        catch(InputMismatchException i)
        {
            System.out.println("\nPTO requests only accept whole hours.");
        }
        catch(ParseException p)
        {
            System.out.println("\nParseException: Error getting the Date " + p.getMessage());
        }
        catch(BadMenuChoiceException b)
        {
            System.out.println("\nBadMenuChoiceException: " + b.getMessage());
        }
        return menuChoice;
    }

    /**
     * menu shown to regular employees. Has limited features
     * @return boolean based on what the menu option entered
     * @throws Exception
     */
    public static boolean employeeMenu() throws Exception
    {
        System.out.println();
        String tempMenuChoice;
        boolean menuChoice = true;
        System.out.println("1. Clock In");//done
        System.out.println("2. Clock Out");//done
        System.out.println("3. View PTO");//done
        System.out.println("4. Request PTO");//done
        System.out.println("0. Logout");//done
        System.out.print("Enter a menu option: ");
        tempMenuChoice = scnr.nextLine();
        tempMenuChoice.trim();
        boolean searchMenuChoice;
        long tempClockTime = 0;
        long tempHoursForClock = 0; //get date and hours from clock in and out
        long tempDateForClock = 0;
        DateFormat formatForDate = new SimpleDateFormat("MM/dd/yyyy");
        DateFormat formatForHours = new SimpleDateFormat("HH:mm");
        Date dateCreator = new Date();
        String stringOfDate = new String();
        String stringOfHours = new String();
        Punch tempPunch = null;
        try
        {
            if(tempMenuChoice.equals("1"))
            {
                tempClockTime = System.currentTimeMillis();

                dateCreator = new Date(tempClockTime);
                stringOfDate = formatForDate.format(dateCreator);
                dateCreator= formatForDate.parse(stringOfDate);
                tempDateForClock  = dateCreator.getTime();

                dateCreator = new Date(tempClockTime);
                stringOfHours = formatForHours.format(dateCreator);
                dateCreator = formatForHours.parse(stringOfHours);
                tempHoursForClock = dateCreator.getTime();

                tempPunch = currUser.getLastPunch();
                if(tempPunch == null)
                {
                    Punch newPunch = new Punch(tempDateForClock, true, tempHoursForClock);
                    currUser.addPunch(newPunch);
                    System.out.println("You have been clocked in at " + newPunch.toString());
                }
                else if(tempPunch.getDate() == tempDateForClock)
                {
                    System.out.println("You already have a clock for today.");
                } 
                else
                {
                    Punch newPunch = new Punch(tempDateForClock, true, tempHoursForClock);
                    currUser.addPunch(newPunch);
                    System.out.println("You have been clocked in at " + newPunch.toString());
                }
            }
            else if(tempMenuChoice.equals("2"))
            {
                tempClockTime = System.currentTimeMillis();

                dateCreator = new Date(tempClockTime);
                stringOfDate = formatForDate.format(dateCreator);
                dateCreator= formatForDate.parse(stringOfDate);
                tempDateForClock  = dateCreator.getTime();

                dateCreator = new Date(tempClockTime);
                stringOfHours = formatForHours.format(dateCreator);
                dateCreator = formatForHours.parse(stringOfHours);
                tempHoursForClock = dateCreator.getTime();
                
                tempPunch = currUser.getLastPunch();
                if(tempPunch == null)
                {
                    Punch newPunch = new Punch(tempDateForClock, false, tempHoursForClock);
                    currUser.addPunch(newPunch);
                    System.out.println("You have been clocked out at " + newPunch.toString());
                }
                else if(tempPunch.getDate() == tempDateForClock && tempPunch.getOutTime() == 0)
                {
                    tempPunch.setOutTime(tempHoursForClock);
                    System.out.println("You have been clocked out at " + tempPunch.toString());
                }
                else if(tempPunch.getDate() == tempDateForClock && tempPunch.getOutTime() != 0)
                {
                    tempPunch.setOutTime(tempHoursForClock);
                    System.out.println("You already have a clock for today");
                }
                else
                {
                    Punch newPunch = new Punch(tempDateForClock, false, tempHoursForClock);
                    currUser.addPunch(newPunch);
                    System.out.println("You have been clocked out at " + newPunch.toString());
                }  
            }
            else if(tempMenuChoice.equals("3"))
            {
                do
                {
                    searchMenuChoice = viewPTO();
                }
                while(searchMenuChoice);
            }
            else if(tempMenuChoice.equals("4"))
            {
                int PTOHours;
                String PTODate;
                String PTOType;
                String newPTORequst;
                System.out.print("Enter the date you would like to use PTO (MM/dd/yyyy): ");
                PTODate = scnr.nextLine().trim();
                dateCreator = formatForDate.parse(PTODate);
                System.out.print("Enter the amount of hours you would like to use on " + PTODate + ": ");
                PTOHours = Integer.parseInt(scnr.nextLine().trim());
                System.out.print("What type of PTO would you like to use? (V/S): ");
                PTOType = scnr.nextLine().trim();
                if(PTOType.toLowerCase().equals("v"))
                {
                    newPTORequst = new String(currUser.getEmployeeID() + "," + PTODate + "," + PTOHours + "," + PTOType + ",Pending");
                    PTORequests.add(newPTORequst.split(","));
                }
                else if(PTOType.toLowerCase().equals("s"))
                {
                    newPTORequst = new String(currUser.getEmployeeID() + "," + PTODate + "," + PTOHours + "," + PTOType + ",Pending");
                    PTORequests.add(newPTORequst.split(","));
                }
                else
                {
                    throw new BadMenuChoiceException("Input was not of type Vacation (V) or Sick (S).");
                }
                System.out.println("PTO Request submitted.");
            }
            else if(tempMenuChoice.equals("0"))
            {
                menuChoice = false;
            }
            else
            {
                throw new BadMenuChoiceException("Input was not a menu option.");
            }
        }
        catch(NumberFormatException n)
        {
            System.out.println("Employee does not exist or hours entered was not a whole hour.");
        }
        catch(ParseException p)
        {
            System.out.println("ParseException: Error getting Date " + p.getMessage());
        }
        catch(BadMenuChoiceException b)
        {
            System.out.println("BadMenuChoiceException: " + b.getMessage());
        }
        return menuChoice;
    }

    /**
     * import submenu used for importing additional data
     * @return boolean based on what menu option is chosen
     */
    public static boolean importMenu()
    {
        System.out.println();
        String tempMenuChoice;
        boolean menuChoice = true;
        System.out.println("1. Import Employee Data");
        System.out.println("2. Import Punch Times");
        System.out.println("3. Import Requested PTO");
        System.out.println("4. Import Time Off");
        System.out.println("0. Go back to Menu");
        System.out.print("Enter a menu option: ");
        tempMenuChoice = scnr.nextLine();
        String filename = new String();
        tempMenuChoice.trim();
        try
        {
            if(tempMenuChoice.equals("1"))
            {
                System.out.print("Enter the name of an employee data file: ");
                filename = scnr.nextLine();
                loadEmployees(filename);
                System.out.println(filename + " file loaded");
            }
            else if(tempMenuChoice.equals("2"))
            {
                System.out.print("Enter the name of a punch time data file: ");
                filename = scnr.nextLine();
                loadPunchTimes(filename);
                System.out.println(filename + " file loaded");
            }
            else if(tempMenuChoice.equals("3"))
            {
                System.out.print("Enter the name of a PTO request data file: ");
                filename = scnr.nextLine();
                loadPTORequests(filename);
                System.out.println(filename + " file loaded");
            }
            else if(tempMenuChoice.equals("4"))
            {
                System.out.print("Enter the name of a time off data file: ");
                filename = scnr.nextLine();
                loadTimeOff(filename);
                System.out.println(filename + " file loaded");
            }
            else if(tempMenuChoice.equals("0"))
            {
                menuChoice = false;
            }
            else
            {
                throw new BadMenuChoiceException("Input was not an import menu choice\n");
            }
        }
        catch(ParseException p)
        {
            System.out.println("ParseException: " + p.getMessage());
        }
        catch(FileNotFoundException f)
        {
            System.out.println("FileNotFoundException: " + f.getMessage());
        }
        catch(NumberFormatException n)
        {
            System.out.println("NumberFormatException: " + n.getMessage());
        }
        catch(BadMenuChoiceException b)
        {
            System.out.println("BadMenuChoiceException: " + b.getMessage());
        }
        return menuChoice;
    }

    /**
     * complete punch submenu that is used to add and complete punches
     * @return boolean based on menu choice
     */
    public static boolean addChangePunch()
    {
        System.out.println();
        String tempMenuChoice;
        boolean menuChoice = true;
        long tempID = 0;
        Employee tempEmployee = null;
        Date dateCreator;
        DateFormat formatForPunchDate = new SimpleDateFormat("MM/dd/yyyy");
        DateFormat formatForPunchHour = new SimpleDateFormat("HH:mm");
        long punchDateInMilli = 0;
        long punchHourInMilli = 0;
        Punch tempPunch = null;
        System.out.println("1. Add Punch");
        System.out.println("2. View/Complete Punch");
        System.out.println("0. Go back to Menu");
        System.out.print("Enter a menu option: ");
        tempMenuChoice = scnr.nextLine();
        tempMenuChoice.trim();
        try
        {
            if(tempMenuChoice.equals("1"))
            {
                System.out.print("Enter the Employee ID for the employee you want to add a punch for: ");
                tempID = Long.parseLong(scnr.nextLine().trim());
                tempEmployee = userLocation.searchEmployees(tempID);
                if(tempEmployee == null) throw new NumberFormatException();
                else if(tempEmployee.getEmployeeID() == currUser.getEmployeeID()) System.out.println("You cannot add your own Punches.");
                else
                {
                    System.out.println(tempEmployee.printPunches());
                    System.out.print("Enter the date of the punch you want to change (MM/dd/yyyy): ");
                    dateCreator = formatForPunchDate.parse(scnr.nextLine().trim());
                    punchDateInMilli = dateCreator.getTime();
                    System.out.print("Enter the In-Time for this Punch(HH:mm): ");
                    dateCreator = formatForPunchHour.parse(scnr.nextLine().trim());
                    punchHourInMilli = dateCreator.getTime();
                    tempPunch  = new Punch(punchDateInMilli, true, punchHourInMilli);
                    System.out.print("Enter the Out-Time for this Punch(HH:mm): ");
                    dateCreator = formatForPunchHour.parse(scnr.nextLine().trim());
                    punchHourInMilli = dateCreator.getTime();
                    tempPunch.setOutTime(punchHourInMilli);
                    tempEmployee.addPunch(tempPunch);
                    System.out.println(tempEmployee.printPunches());
                }
            }
            else if(tempMenuChoice.equals("2"))
            {
                System.out.print("Enter the Employee ID for the employee you want to change a punch for: ");
                tempID = Long.parseLong(scnr.nextLine().trim());
                tempEmployee = userLocation.searchEmployees(tempID);
                if(tempEmployee == null) throw new NumberFormatException();
                else if(tempEmployee.getEmployeeID() == currUser.getEmployeeID()) System.out.println("You cannot change your own Punches.");
                else
                {
                    System.out.print("Enter the date of the punch you want to change (MM/dd/yyyy): ");
                    dateCreator = formatForPunchDate.parse(scnr.nextLine().trim());
                    punchDateInMilli = dateCreator.getTime();
                    tempPunch = tempEmployee.findPunch(punchDateInMilli);
                    if(tempPunch == null) System.out.println("Punch does not exist for that date.");
                    else
                    {
                        
                        if(tempPunch.getInTime() != 0 && tempPunch.getOutTime() != 0) System.out.println("This Punch is completed.");
                        if(tempPunch.getInTime() == 0) 
                        {
                            System.out.println(tempPunch);
                            System.out.print("Enter the In-Time for this Punch(HH:mm): ");
                            dateCreator = formatForPunchHour.parse(scnr.nextLine().trim());
                            punchHourInMilli = dateCreator.getTime();
                            tempPunch.setInTime(punchHourInMilli);
                        }
                        else if(tempPunch.getOutTime() == 0)
                        {
                            System.out.println(tempPunch);
                            System.out.print("Enter the Out-Time for this Punch(HH:mm): ");
                            dateCreator = formatForPunchHour.parse(scnr.nextLine().trim());
                            punchHourInMilli = dateCreator.getTime();
                            tempPunch.setOutTime(punchHourInMilli);
                        }
                        System.out.println(tempPunch);
                    }
                }
            }
            else if(tempMenuChoice.equals("0"))
            {
                menuChoice = false;
            }
            else
            {
                throw new BadMenuChoiceException("Input was not an import menu choice\n");
            }
        }
        catch(ParseException p)
        {
            System.out.println("ParseException: " + p.getMessage());
        }
        catch(NumberFormatException n)
        {
            System.out.println("EmpID does not exist.\n");
        }
        catch(BadMenuChoiceException b)
        {
            System.out.println("BadMenuChoiceException: " + b.getMessage() + "\n");
        }
        return menuChoice;
    }

    /**
     * view pto submenu that allows you to view your own avaliable PTO and used PTO
     * @return
     */
    public static boolean viewPTO()
    {
        System.out.println();
        String tempMenuChoice;
        boolean menuChoice = true;
        System.out.println("1. View avaliable PTO");
        System.out.println("2. View used PTO");
        System.out.println("0. Go back to Menu");
        System.out.print("Enter a menu option: ");
        tempMenuChoice = scnr.nextLine();
        tempMenuChoice.trim();
        try
        {
            if(tempMenuChoice.equals("1"))
            {
                System.out.println(currUser.printPTO());
            }
            else if(tempMenuChoice.equals("2"))
            {
                System.out.println(currUser.printUsedPTO());
            }
            else if(tempMenuChoice.equals("0"))
            {
                menuChoice = false;
            }
            else
            {
                throw new BadMenuChoiceException("Input was not an import menu choice\n");
            }
        }
        catch(NumberFormatException n)
        {
            System.out.println("EmpID does not exist.\n");
        }
        catch(BadMenuChoiceException b)
        {
            System.out.println("BadMenuChoiceException: " + b.getMessage() + "\n");
        }
        return menuChoice;
    }

    /**
     * view employee PTO sub menu that allows you to view another employees avaliable PTO and used PTO
     */
    public static boolean viewEmployeePTO()
    {
        System.out.println();
        String tempMenuChoice;
        boolean menuChoice = true;
        System.out.println("1. View avaliable PTO");
        System.out.println("2. View used PTO");
        System.out.println("0. Go back to Menu");
        System.out.print("Enter a menu option: ");
        tempMenuChoice = scnr.nextLine();
        Employee tempEmp = null;
        long tempID = 0;
        tempMenuChoice.trim();
        try
        {
            if(tempMenuChoice.equals("1"))
            {
                System.out.print("Enter an EmpID: ");
                tempID = Long.parseLong(scnr.nextLine());
                tempEmp = null;
                for(Location tempLocation : locations)
                {
                    tempEmp = tempLocation.searchEmployees(tempID);
                    if(tempEmp == null) continue;
                    else break;
                }
                if(tempEmp == null) throw new NumberFormatException();
                else
                {
                    System.out.println(tempEmp.printPTO());
                }
            }
            else if(tempMenuChoice.equals("2"))
            {
                System.out.print("Enter an EmpID: ");
                tempID = Long.parseLong(scnr.nextLine());
                tempEmp = null;
                for(Location tempLocation : locations)
                {
                    tempEmp = tempLocation.searchEmployees(tempID);
                    if(tempEmp == null) continue;
                    else break;
                }
                if(tempEmp == null) throw new NumberFormatException();
                else
                {;
                    System.out.println(tempEmp.printUsedPTO());
                }
            }
            else if(tempMenuChoice.equals("0"))
            {
                menuChoice = false;
            }
            else
            {
                throw new BadMenuChoiceException("Input was not an import menu choice\n");
            }
        }
        catch(NumberFormatException n)
        {
            System.out.println("EmpID does not exist.\n");
        }
        catch(BadMenuChoiceException b)
        {
            System.out.println("BadMenuChoiceException: " + b.getMessage() + "\n");
        }
        return menuChoice;
    }

    /**
     * view PTORequests submenu that allows you to see and approve/decline PTO requests
     */
    public static boolean viewPTORequests()
    {
        System.out.println();
        String tempMenuChoice;
        String[] PTORequest = null;
        String strID;
        String strDate;
        boolean menuChoice = true;
        boolean counter;
        DateFormat formatForDate = new SimpleDateFormat("MM/dd/yyyy");
        Date dateCreator;
        long tempDate;
        long tempID;
        int tempHours;
        String typeOfPTO;
        Employee tempEmployee = null;
        boolean enoughPTO;
        String overrideDecision;
        Punch tempPunch;
        System.out.println("1. View PTO Requests");
        System.out.println("2. Approve a PTO Request");
        System.out.println("3. Decline a PTO Request");
        System.out.println("0. Go back to Menu");
        System.out.print("Enter a menu option: ");
        tempMenuChoice = scnr.nextLine();
        tempMenuChoice.trim();
        try
        {
            if(tempMenuChoice.equals("1"))
            {
                System.out.println("PTO Requests:");
               for(String[] s: PTORequests)
               {
                   if(userLocation.searchEmployees(Long.parseLong(s[0])) != null) System.out.println(String.join(",",s));
                   
               }
            }
            else if(tempMenuChoice.equals("2"))
            {
                System.out.print("Enter the Employee ID of the PTO Requster: ");
                strID = scnr.nextLine().trim();
                if(Long.parseLong(strID) == currUser.getEmployeeID()) throw new PTORequestDoesNotExistException("Cannot approve your own PTO request.");
                System.out.print("Enter the date of the PTO Request: ");
                strDate = scnr.nextLine().trim();

                tempID = Long.parseLong(strID);
                tempEmployee = userLocation.searchEmployees(tempID);
                if(tempEmployee == null) throw new PTORequestDoesNotExistException("This employee does not Exist at your location.");

                for(String[] s: PTORequests)
                {
                    if(s[0].equals(strID) && s[1].equals(strDate)) PTORequest = s;
                }
                if(PTORequest == null) throw new PTORequestDoesNotExistException("A PTO Request from " + strID + " on " + strDate + " does not exist.");
                
                dateCreator = formatForDate.parse(strDate);
                tempDate = dateCreator.getTime();
                typeOfPTO = PTORequest[3];
                tempHours = Integer.parseInt(PTORequest[2]);
                if(typeOfPTO.toLowerCase().equals("v"))
                {
                    enoughPTO = tempEmployee.checkVacationTimeHours(tempHours);
                    if(enoughPTO == false)
                    {
                        while(true)
                        {
                            System.out.print("Employee " + strID + " does not have enough vacation time accumulated for this request. Would you like to override? (Y/N): ");
                            overrideDecision = scnr.nextLine().trim();
                            if(overrideDecision.toLowerCase().equals("y")) 
                            {
                                enoughPTO = true;
                                break;
                            }
                            else if(overrideDecision.toLowerCase().equals("n")) break;
                        }   
                    }
                    if(enoughPTO)
                    {
                        tempEmployee.useVacationTime(tempHours, strDate);
                        tempPunch = new Punch(tempDate, true, 50400000L); //creating punch that starts at 8am
                        tempPunch.setOutTime(tempPunch.getInTime() + (long)((long)tempHours * 3600000L));
                        tempEmployee.addPunch(tempPunch);
                    }
                }
                else if(typeOfPTO.toLowerCase().equals("s"))
                {
                    enoughPTO = tempEmployee.checkSickTimeHours(tempHours);
                    if(enoughPTO == false)
                    {
                        while(true)
                        {
                            System.out.print("Employee " + strID + " does not have enough sick time accumulated for this request. Would you like to override? (Y/N): ");
                            overrideDecision = scnr.nextLine().trim();
                            if(overrideDecision.toLowerCase().equals("y")) 
                            {
                                enoughPTO = true;
                                break;
                            }
                            else if(overrideDecision.toLowerCase().equals("n")) break;
                        }   
                    }
                    if(enoughPTO)
                    {
                        tempEmployee.useSickTime(tempHours, strDate);
                        tempPunch = new Punch(tempDate, true, 50400000L); //creating punch that starts at 8am
                        tempPunch.setOutTime(tempPunch.getInTime() + (long)((long)tempHours * 3600000L));
                        tempEmployee.addPunch(tempPunch);
                    }
                }
                PTORequests.remove(PTORequest);
                System.out.println("PTO request Approved.");
            }
            else if(tempMenuChoice.equals("3"))
            {
                System.out.print("Enter the Employee ID of the PTO Requster: ");
                strID = scnr.nextLine().trim();
                if(Long.parseLong(strID) == currUser.getEmployeeID()) throw new PTORequestDoesNotExistException("Cannot decline your own PTO request.");
                System.out.print("Enter the date of the PTO Request: ");
                strDate = scnr.nextLine().trim();

                tempID = Integer.parseInt(strID);
                tempEmployee = userLocation.searchEmployees(tempID);
                if(tempEmployee == null) throw new PTORequestDoesNotExistException("This employee does not Exist at your location.");
                
                for(String[] s :PTORequests)
                {
                    if(s[0].equals(strID) && s[1].equals(strDate)) PTORequest = s;
                }
                if(PTORequest == null) throw new PTORequestDoesNotExistException("A PTO Request from " + strID + " on " + strDate + " does not exist.");
                PTORequests.remove(PTORequest);
                System.out.println("PTO Request declined.");
            }
            else if(tempMenuChoice.equals("0"))
            {
                menuChoice = false;
            }
            else
            {
                throw new BadMenuChoiceException("Input was not an import menu choice\n");
            }
        }
        catch(PTORequestDoesNotExistException p)
        {
            System.out.println("PTORequestDoesNotExistException: " + p.getMessage());
        }
        catch(ParseException p)
        {
            System.out.println("ParseException: " + p.getMessage());
        }
        catch(NumberFormatException n)
        {
            System.out.println("EmpID does not exist or hours entered was not a whole hour.\n");
        }
        catch(BadMenuChoiceException b)
        {
            System.out.println("BadMenuChoiceException: " + b.getMessage() + "\n");
        }
        return menuChoice;
    }

    /**
     * loads employee data, called from importMenu() and loadCommandLineFiles()
     * @param filename
     * @throws FileNotFoundException
     * @throws NumberFormatException
     */
    private static void loadEmployees(String filename) throws FileNotFoundException, NumberFormatException
    {
        int countLoaded = 0; //Let's count how many students are added just to make sure it works
        Scanner filescanner = new Scanner(new File(filename)); //will throw exception if file not found
        while(filescanner.hasNext()) { //Make sure there are more lines to load
            String line = filescanner.nextLine(); //read in the line
            String[] values = line.split(","); //make an array of the strings, split on the comma
            if(!values[0].equals("EmpID")){//Don't make a studentNode from the header row in the CSV
                Location temp = new Location(values[4].trim());
                if(locations.contains(temp))
                {
                    int tempIndex = locations.indexOf(temp);
                    addEmployeeToLocation(locations.get(tempIndex), values);
                } 
                else
                {
                    locations.add(temp);
                    int tempIndex = locations.indexOf(temp);
                    addEmployeeToLocation(locations.get(tempIndex), values);
                }
                countLoaded++;
            }
        } //end of while loop for scanning the file
        // System.out.println("Done loading data. " + countLoaded + " employees added."); //Let user know it worked
        filescanner.close();
    }

    /**
     * adds employees to a location based on their position. called from loadEmployees()
     */
    static void addEmployeeToLocation(Location l, String[] values) throws NumberFormatException
    {
        if(values[3].equals("Employee"))
        {
            l.addEmployee(new Employee(Long.parseLong(values[0]), values[1], values[2], values[3], l));
        }
        else
        {
            l.addEmployee(new Supervisor(Long.parseLong(values[0]), values[1], values[2], values[3], l));
        }
    }

    /**
     * loads punches from .csv files. called from importMenu() and loadCommandLineFiles()
     * @param filename
     * @throws FileNotFoundException
     * @throws ParseException
     * @throws NumberFormatException
     */
    private static void loadPunchTimes(String filename) throws FileNotFoundException, ParseException, NumberFormatException
    {
        String tempStringDate;
        String tempStringHours;
        SimpleDateFormat tempDateFormat;
        Date tempDate;
        long dateInMilli;
        SimpleDateFormat tempHourFormat;
        Date tempHours;
        long hoursInMilli;
        long tempID;
        boolean io;
        Punch tempPunch;
        Employee tempEmployee = null;
        int countLoaded = 0; //Let's count how many students are added just to make sure it works
        Scanner filescanner = new Scanner(new File(filename)); //will throw exception if file not found
        while(filescanner.hasNext()) { //Make sure there are more lines to load
            String line = filescanner.nextLine(); //read in the line
            String[] values = line.split(","); //make an array of the strings, split on the comma
            if(!values[0].equals("EmpID")){//Don't make a studentNode from the header row in the CSV
                
                //get date
                tempStringDate = values[1];
                tempDateFormat = new SimpleDateFormat("MM/dd/yyyy");
                tempDate = tempDateFormat.parse(tempStringDate);
                dateInMilli = tempDate.getTime();

                //get hours
                tempStringHours = values[2];
                tempHourFormat = new SimpleDateFormat("HH:mm");
                tempHours = tempHourFormat.parse(tempStringHours);
                hoursInMilli = tempHours.getTime();

                tempID = Long.parseLong(values[0]);
                io = values[3].equals("i") ? true : false; //true is in, false is out
                
                for(Location tempLocation : locations)
                {
                    tempEmployee = tempLocation.searchEmployees(tempID);
                    if(tempEmployee == null) continue;
                    else break;
                }
                if(io == true) //clock in
                {
                    tempPunch = tempEmployee.getLastPunch();
                    if(tempPunch == null);
                    // else if(tempPunch.getOutTime() == 0) System.out.println("Employee " + tempID + " forgot to clock-out on " + tempPunch.getFormattedDate());
                    tempEmployee.addPunch(new Punch(dateInMilli, io, hoursInMilli));
                }
                else // clock out
                {
                    tempPunch = tempEmployee.getLastPunch();
                    if(tempPunch == null) tempEmployee.addPunch(new Punch(dateInMilli, io, hoursInMilli));
                    else if(tempPunch.getDate() != dateInMilli)
                    { 
                        // System.out.println("Employee " + tempID + " forgot to clock-in on " + tempStringDate);
                        tempEmployee.addPunch(new Punch(dateInMilli, io, hoursInMilli));
                    }
                    else tempPunch.setOutTime(hoursInMilli);
                }
                countLoaded++; 
            }
            
        }
        // System.out.println("Done loading data. " + countLoaded + " punches added."); //Let user know it worked
        filescanner.close();
    }
    
    /**
     * loads PTO requests from .csv Files. Called from importMenu() and loadCommandLineFiles()
     */
    private static void loadPTORequests(String filename) throws FileNotFoundException, NumberFormatException, ParseException
    {
        String tempStringDate;
        SimpleDateFormat tempDateFormat;
        SimpleDateFormat tempHourFormat;
        Date tempDate;
        long dateInMilli;
        long tempID;
        Employee tempEmployee = null;
        Punch tempPunch;
        int hoursRequested;
        int countLoaded = 0; //Let's count how many students are added just to make sure it works
        Scanner filescanner = new Scanner(new File(filename)); //will throw exception if file not found
        while(filescanner.hasNext()) { //Make sure there are more lines to load
            String line = filescanner.nextLine(); //read in the line
            String[] values = line.split(","); //make an array of the strings, split on the comma
            if(!values[0].equals("EmpID")){//Don't make a studentNode from the header row in the CSV
                tempID = Long.parseLong(values[0]);
                tempStringDate = values[1];
                tempDateFormat = new SimpleDateFormat("MM/dd/yyyy");
                tempHourFormat = new SimpleDateFormat("HH");
                tempDate = tempDateFormat.parse(tempStringDate);
                dateInMilli = tempDate.getTime();
                for(Location tempLocation : locations)
                {
                    tempEmployee = tempLocation.searchEmployees(tempID);
                    if(tempEmployee == null) continue;
                    else break;
                }
                if(values[3].equals("V")) //Vacation
                {
                    if(dateInMilli < System.currentTimeMillis() && values[4].equals("Approved")) tempEmployee.addUsedPTO(new VacationTime(Integer.parseInt(values[2]), values[1]));//add to used PTO
                    else if(dateInMilli >= System.currentTimeMillis() && values[4].equals("Approved")) //create a punch for that PTO request
                    {
                        tempPunch = new Punch(dateInMilli, true, 50400000L); //creating punch that starts at 8am
                        hoursRequested = Integer.parseInt(values[2]);
                        tempPunch.setOutTime(tempPunch.getInTime() + (long)((long)hoursRequested * 3600000L)); //number of milliseconds in an hour
                        tempEmployee.addPunch(tempPunch);
                        tempEmployee.addUsedPTO(new VacationTime(Integer.parseInt(values[2]), values[1]));
                    } 
                    else PTORequests.add(values); //add to PTORequests arrayList for viewing later
                }
                else //Sick
                {  
                    if(dateInMilli < System.currentTimeMillis() && values[4].equals("Approved")) tempEmployee.addUsedPTO(new SickTime(Integer.parseInt(values[2]), values[1])); //add to used PTO
                    else if(dateInMilli >= System.currentTimeMillis() && values[4].equals("Approved"))//create a punch for that PTO request
                    {
                        tempPunch = new Punch(dateInMilli, true, 50400000L); //creating punch that starts at 8am
                        hoursRequested = Integer.parseInt(values[2]);
                        tempPunch.setOutTime(tempPunch.getInTime() + (long)((long)hoursRequested * 3600000L));
                        tempEmployee.addPunch(tempPunch);
                        tempEmployee.addUsedPTO(new SickTime(Integer.parseInt(values[2]), values[1]));
                    }
                    else PTORequests.add(values); //add to PTORequests arrayList for viewing later
                }
                countLoaded++;
            }
        } //end of while loop for scanning the file
        // System.out.println("Done loading data. " + countLoaded + " PTO requests added."); //Let user know it worked
        filescanner.close();
    }

    /**
     * loads time off data from .csv files. Called from importMenu() and loadCommandLineFiles()
     * @param filename
     * @throws FileNotFoundException
     * @throws ParseException
     * @throws NumberFormatException
     */
    private static void loadTimeOff(String filename) throws FileNotFoundException, ParseException, NumberFormatException
    {
        String PTOType;
        int amountOfHours;
        long tempID;
        Employee tempEmployee = null;
        String strExperationDate;
        DateFormat experationDateFormat;
        Date tempExperationDate;
        long experationDateInMilli;
        int countLoaded = 0; //Let's count how many students are added just to make sure it works
        Scanner filescanner = new Scanner(new File(filename)); //will throw exception if file not found
        while(filescanner.hasNext()) { //Make sure there are more lines to load
            String line = filescanner.nextLine(); //read in the line
            String[] values = line.split(","); //make an array of the strings, split on the comma
            if(!values[0].equals("EmpID")){//Don't make a studentNode from the header row in the CSV
                PTOType = values[1];
                amountOfHours = Integer.parseInt(values[2]);
                tempID = Long.parseLong(values[0]);
                strExperationDate = values[3];
                experationDateFormat = new SimpleDateFormat("MM/dd/yyyy");
                tempExperationDate = experationDateFormat.parse(strExperationDate);
                experationDateInMilli = tempExperationDate.getTime();
                for(Location tempLocation : locations)
                {
                    tempEmployee = tempLocation.searchEmployees(tempID);
                    if(tempEmployee == null) continue;
                    else break;
                }
                if(tempEmployee == null)
                {
                    System.out.println("Employee " + tempID + " does not exist.");
                    continue;
                }
                else
                {
                    if(PTOType.equals("Vacation")) //Vacation type
                    {
                        tempEmployee.addVacationTime(new VacationTime(amountOfHours, experationDateInMilli));
                    } 
                    else //Sick Type
                    {
                        tempEmployee.addSickTime(new SickTime(amountOfHours, experationDateInMilli));
                    }
                }
                countLoaded++;
            }
        } //end of while loop for scanning the file
        // System.out.println("Done loading data. " + countLoaded + " PTO added."); //Let user know it worked
        filescanner.close();
    }

    /**
     * exports employee data to a .csv. Called from exportMenu()
     */
    static public void exportEmployeeData()
    {
        String employeeData = new String("EmpID,Fname,Lname,Position,Location\n");
        String filename = "employeedata-";
        DateFormat formatForEmployeeDate = new SimpleDateFormat("MM-dd-yyyy");
        Date dateCreator = new Date(System.currentTimeMillis());
        filename += formatForEmployeeDate.format(dateCreator) + ".csv";
		try{
            PrintWriter out = new PrintWriter(new File(filename));
            for(Location l:locations)
            {
                employeeData += l.getEmployeeData();
            }
            out.println(employeeData);
            System.out.println("Employee data exported to " + filename);
			out.close();
		}
		catch (Exception e){
			System.out.println("Something went wrong. " + e);
		}
    }
    
    /**
     * exports time off data to a .csv. Called from exportMenu()
     */
    static public void exportTimeOffData()
    {
        String timeOffData = new String("EmpID,Type,Amount,Expiration Date\n");
        String filename = "timeoffdata-";
        DateFormat formatForEmployeeDate = new SimpleDateFormat("MM-dd-yyyy");
        Date dateCreator = new Date(System.currentTimeMillis());
        filename += formatForEmployeeDate.format(dateCreator) + ".csv";
		try{
            PrintWriter out = new PrintWriter(new File(filename));
            for(Location l:locations)
            {
                timeOffData += l.getTimeOffData();
            }
            out.println(timeOffData);
            System.out.println("Time off data exported to " + filename);
			out.close();
		}
		catch (Exception e){
			System.out.println("Something went wrong. " + e);
		}
    }

    /**
     * exports PTO request data to a .csv file. Called form exportMenu()
     */
    static public void exportPTORequests()
    {
        String ptoRequestData = new String("EmpID,Date Requested Off,Hours Requested Off,Type (V/S),Status\n");
        String filename = "PTORequestdata-";
        DateFormat formatForEmployeeDate = new SimpleDateFormat("MM-dd-yyyy");
        Date dateCreator = new Date(System.currentTimeMillis());
        filename += formatForEmployeeDate.format(dateCreator) + ".csv";
		try{
            PrintWriter out = new PrintWriter(new File(filename));
            for(String[] s: PTORequests)
            {
                ptoRequestData += String.join(",", s) + "\n";
            }
            out.println(ptoRequestData);
            System.out.println("PTO Request data exported to " + filename);
			out.close();
		}
		catch (Exception e){
            System.out.println("Something went wrong. " + e);
		}
    }

    /**
     * exports punch data to a .csv file. Called from exportMenu()
     */
    static public void exportPunches()
    {
        String exportData = new String("EmpID,Date,time,inOrOut\n");
        String filename = "punchdata-";
        DateFormat formatForEmployeeDate = new SimpleDateFormat("MM-dd-yyyy");
        Date dateCreator = new Date(System.currentTimeMillis());
        filename += formatForEmployeeDate.format(dateCreator) + ".csv";
		try{
            PrintWriter out = new PrintWriter(new File(filename));
            for(Location l:locations)
            {
                exportData += l.getPunchData();
            }
            out.println(exportData);
            System.out.println("Punch data exported to " + filename);
			out.close();
		}
		catch (Exception e){
			System.out.println("Something went wrong. " + e);
		}
    }
}


class Employee
{
    protected Location homeLocation;
    protected SickTime sickTimeHead = null; //max length will be 52
    protected VacationTime vacaTimeHead = null; //max length will be 52
    protected ArrayList<PTO> usedPTO = new ArrayList<PTO>();
    protected SickTime sickTimeTail;
    protected VacationTime vacaTimeTail;
    protected ArrayList<Punch> punches = new ArrayList<Punch>(14);
    protected double payRate = 20;
    protected String firstName;
    protected String lastName;
    protected Long employeeID;
    protected String position;
    protected Employee right = null;
    protected Employee left = null;
    protected int sickTimeIncrement = 2;
    protected int vacationTimeIncrement = 2;
    protected int minHoursForPTO = 5;
    protected Timecard timeCard = new Timecard();

    /**
     * default constructor
     */
    public Employee() {}

    /**
     * main constructor that is used to initialize an employee
     * @param EID employeeID
     * @param fn first name
     * @param ln last name
     * @param p position
     * @param l location
     */
    public Employee(long EID, String fn, String ln, String p, Location l)
    {
        employeeID = EID;
        firstName = fn;
        lastName = ln;
        position = p;
        homeLocation = l;
    }

    /**
     * overriden toString() method for printing
     */
    public String toString()
    {
        return new String(employeeID + " " + firstName + " " + lastName + ", " + position + " at " + homeLocation.getLocationName() + "\n");
    }

    /**
     * used to convert employee to csv format
     * @return
     */
    public String toCSV()
    {
        return new String(employeeID +"," + firstName + "," + lastName + "," + position + "," + homeLocation.getLocationName() + "\n");
    } 

    /**
     * used to convert all employees at a location to a csv format
     * @return
     */
    public String getEmployeeDataAsCSV()
    {
        String temp = new String();
        if(left != null) temp += left.getEmployeeDataAsCSV();
        temp += this.toCSV();
        if(right != null) temp += right.getEmployeeDataAsCSV();
        return temp;
    }

    /**
     * used to convert all time off data at a location to a csv format
     * @return
     */
    public String getTimeOffDataAsCSV()
    {
        String temp = new String();
        VacationTime tempVacationTime = vacaTimeHead;
        SickTime tempSickTime = sickTimeHead;
        if(left != null) temp +=  left.getTimeOffDataAsCSV();
        while(tempSickTime != null)
        {   
            temp += employeeID + "," + "Sick" + "," + tempSickTime.getHours() + "," + tempSickTime.getFormattedDate() + "\n";
            tempSickTime = tempSickTime.getNext();
        } 
        while(tempVacationTime != null)
        {
            temp += employeeID + "," + "Vacation" + "," + tempVacationTime.getHours() + "," + tempVacationTime.getFormattedDate() + "\n";
            tempVacationTime = tempVacationTime.getNext();
        } 
        if(right != null) temp += right.getTimeOffDataAsCSV();
        return temp;
    }

    /**
     * used to convert all punch data at a location to a csv format
     * @return
     */
    public String getPunchDataAsCSV()
    {
        String temp = new String();
        if(left != null) temp += left.getPunchDataAsCSV();
        for(Punch p: punches)
        {
            if(p.getInTime() == 0)
            {
                temp += employeeID + "," + p.getFormattedDate() + ",NO CLOCK IN,i\n";
                temp += employeeID + "," + p.getFormattedDate() + "," + p.getFormattedOutTime() + ",o\n";
            }
            else if(p.getOutTime() == 0)
            {
                temp += employeeID + "," + p.getFormattedDate() + "," + p.getFormattedInTime() + ",i\n";
                temp += employeeID + "," + p.getFormattedDate() + ",NO CLOCK OUT,o\n";
            }
            else
            {
                temp += employeeID + "," + p.getFormattedDate() + "," + p.getFormattedInTime() + ",i\n";
                temp += employeeID + "," + p.getFormattedDate() + "," + p.getFormattedOutTime() + ",o\n";
            }
        }
        if(right != null) temp += right.getPunchDataAsCSV();
        return temp;
    }

    /**
     * used for testing to print all employees to screen
     */
    public String printEmployee()
    {
        String temp = new String();
        if(left != null) temp += left.printEmployee();
        temp += this.toString();
        if(right != null) temp += right.printEmployee();
        return temp;
    }

    /**
     * used for testing to print all punches to screen
     * @return
     */
    public String printPunches()
    {
        String temp = new String();
        // if(left != null) temp += left.printPunches();
        temp += this.toString();
        for(Punch p: punches) temp += p.toString();
        // if(right != null) temp += right.printPunches();
        return temp;
    }

    /**
     * used for testing to print all PTO to screen
     * @return
     */
    public String printAllPTO()
    {
        String tempString = new String();
        if(left != null) tempString += left.printPTO();
        tempString += this.printPTO();
        if(right != null) tempString += right.printPTO();
        return tempString;
    }

    /**
     * used for testing to print induvidual PTO to screen
     * @return
     */
    public String printPTO()
    {
        SickTime tempSickTime = sickTimeHead;
        VacationTime tempVacationTime = vacaTimeHead; 
        String tempString = new String();
        tempString += this.toString();
        tempString += "\nSick Time avaliable: \n";
        if(sickTimeHead == null);
        else
        {
            tempString += sickTimeHead.toString();
            while(tempSickTime != sickTimeTail)
            {
                tempString += tempSickTime.getNext().toString();
                tempSickTime = tempSickTime.getNext();
            }
        }
        tempString += "\nVacation Time avaliable: \n";
        if(vacaTimeHead == null);
        else
        {
            tempString += vacaTimeHead.toString();
            while(tempVacationTime != vacaTimeTail)
            {
                tempString += tempVacationTime.getNext().toString();
                tempVacationTime = tempVacationTime.getNext();
            }
        }
        return tempString;
    }

    /***
     * accumulates avaliable vacation time until equivalent to numberOfHours and then sets the node to be used
     * @param numberOfHours number of hours to meet for PTO request
     * @param dateUsed used for setting the date the PTO was used
     */
    public void useVacationTime(int numberOfHours, String dateUsed)
    {
        int hoursCounter = 0;
        int leftOver = 0;
        VacationTime temp = vacaTimeHead;
        long currTime = System.currentTimeMillis();
        while(temp != null && temp.getExpirationDate() > currTime)
        {
            hoursCounter += temp.getHours();
            if(hoursCounter == numberOfHours)
            {
                temp.setDateUsed(dateUsed);
                temp.setUsed(true);
                break;
            }
            else if(hoursCounter > numberOfHours)
            {
                leftOver = hoursCounter - numberOfHours;
                temp.setHours(leftOver);
                break;
            }
            else if(hoursCounter < numberOfHours)
            {
                temp.setDateUsed(dateUsed);
                temp.setUsed(true);
                temp = temp.getNext();
            }
        }    
        cleanUpVacationTime();   
    }

    /***
     * accumulates avaliable sick time until equivalent to numberOfHours and then sets the node to be used
     * @param numberOfHours number of hours to meet for PTO request
     * @param dateUsed used for setting the date the PTO was used
     */
    public void useSickTime(int numberOfHours, String dateUsed)
    {
        int hoursCounter = 0;
        int leftOver = 0;
        SickTime temp = sickTimeHead;
        long currTime = System.currentTimeMillis();
        while(temp != null && temp.getExpirationDate() > currTime)
        {
            hoursCounter += temp.getHours();
            if(hoursCounter == numberOfHours)
            {
                temp.setDateUsed(dateUsed);
                temp.setUsed(true);
                break;
            }
            else if(hoursCounter > numberOfHours)
            {
                leftOver = hoursCounter - numberOfHours;
                temp.setHours(leftOver);
                break;
            }
            else if(hoursCounter < numberOfHours)
            {
                temp.setDateUsed(dateUsed);
                temp.setUsed(true);
                temp = temp.getNext();
            }
        }    
        cleanUpSickTime();
    }

    /**
     * finds all used sick time and moves it to usedPTO and deletes it from the sick time linked list
     */
    public void cleanUpSickTime()
    {
        if(sickTimeHead == null) return;
        while(sickTimeHead != null && sickTimeHead.getUsed() == true) 
        {
            if(sickTimeHead == null) break;
            usedPTO.add(sickTimeHead);
            sickTimeHead = sickTimeHead.getNext();
        }
    }
    
    /**
     * finds all used vacation time and moves it to usedPTO and deletes it from the vacation time linked list
     */
    public void cleanUpVacationTime()
    {
        if(vacaTimeHead == null) return;
        while(vacaTimeHead != null && vacaTimeHead.getUsed() == true) 
        {
            usedPTO.add(vacaTimeHead);
            vacaTimeHead = vacaTimeHead.getNext();
        }
    }

    /**
     * finds if employee has enough vacation time avaliable for a PTO request
     */
    public boolean checkVacationTimeHours(int hoursRequesting)
    {
        boolean enoughHours = false;
        int accumulatedHours = 0;
        VacationTime temp = vacaTimeHead;
        long currTime = System.currentTimeMillis();
        while(temp != null && temp.getExpirationDate() > currTime)
        {
            accumulatedHours += temp.getHours();
            if(accumulatedHours >= hoursRequesting) 
            {
                enoughHours = true;
                break;
            }
            temp = temp.getNext();
        }
        return enoughHours;
    }

    /**
     * finds if employee has enough sick time avaliable for a PTO request
     */
    public boolean checkSickTimeHours(int hoursRequesting)
    {
        boolean enoughHours = false;
        int accumulatedHours = 0;
        SickTime temp = sickTimeHead;
        long currTime = System.currentTimeMillis();
        while(temp != null && temp.getExpirationDate() > currTime)
        {
            accumulatedHours += temp.getHours();
            if(accumulatedHours >= hoursRequesting) 
            {
                enoughHours = true;
                break;
            }
            temp = temp.getNext();
        }
        return enoughHours;
    }

    /**
     * used to recursively get all used PTO for all employees at a location
     * @return
     */
    public String printAllUsedPTO()
    {   
        String temp = new String();
        if(left != null) temp += left.printUsedPTO();
        temp += this.printUsedPTO();
        if(right != null) temp += right.printUsedPTO();
        return temp;
    }

    /**
     * used to get the used PTO for an employee
     * @return
     */
    public String printUsedPTO()
    {
        String temp = new String();
        temp += this.toString();
        temp += "Used PTO: \n";
        for(PTO p: usedPTO)
        {
            temp += p.usedToString();
        } 
        return temp;
    }

    /**
     * adds a PTO that has been used to the usedPTO arrayList
     * @param p
     */
    public void addUsedPTO(PTO p)
    {
        usedPTO.add(p);
    }

    /**
     * sets EID to employeeID
     * @param EID
     */
    public void setEmployeeID(long EID)
    {
        employeeID = EID;
    }

    /**
     * sets payRate to pr
     * @param pr
     */
    public void setPayRate(double pr)
    {
        payRate = pr;
    }
    
    /**
     * sets homeLocation to l
     * @param l
     */
    public void setLocation(Location l)
    {
        homeLocation = l;
    }

    public void setPosition(String p)
    {
        position = p;
    }

    /**
     * return employees first name
     * @return
     */
    public String getFirstName()
    {
        return firstName;
    }

    /**
     * return employess last name
     * @return
     */
    public String getLastName()
    {
        return lastName;
    }

    /**
     * return an employee's ID
     * @return
     */
    public long getEmployeeID()
    {
        return employeeID;
    }

    /**
     * return an employee's pay rate
     * @return
     */
    public double getPayRate()
    {
        return payRate;
    }

    /**
     * returns an employee's home location
     * @return
     */
    public Location getHomeLocation()
    {
        return homeLocation;
    }

    /**
     * returns an employee's position
     * @return
     */
    public String getPosition()
    {
        return position;
    }

    /**
     * returns an employees right leaf in the tree
     * @return
     */
    public Employee getRight()
    {
        return right;
    }

    /**
     * returns an employees left leaf in a tree
     * @return
     */
    public Employee getLeft()
    {
        return left;
    }

    /**
     * generates the time cards for the employee's a shift manager can approve
     * @param beginningOfWeek
     * @param endOfWeek
     */
    public void GenerateShiftManagerTimeCards(long beginningOfWeek, long endOfWeek)
    {
        if(left != null) left.GenerateShiftManagerTimeCards(beginningOfWeek, endOfWeek);
        if(!position.equals("General Manager") && !position.equals("Shift Manager"))
        {
            for(int i = 0; i < punches.size(); ++i)
            {
                if(punches.get(i).getDate() >= beginningOfWeek && punches.get(i).getDate() <= endOfWeek)
                {
                    timeCard.addToTimecard(punches.get(i));
                }
            }
        }
        if(right != null) right.GenerateShiftManagerTimeCards(beginningOfWeek, endOfWeek);
    }

    /**
     * generates the timecards for the shift managers and general manager
     * @param beginningOfWeek
     * @param endOfWeek
     */
    public void GenerateGeneralManagerTimeCards(long beginningOfWeek, long endOfWeek)
    {
        if(left != null) left.GenerateGeneralManagerTimeCards(beginningOfWeek, endOfWeek);
        if(position.equals("General Manager") || position.equals("Shift Manager"))
        {
            for(int i = 0; i < punches.size(); ++i)
            {
                if(punches.get(i).getDate() >= beginningOfWeek && punches.get(i).getDate() <= endOfWeek)
                {
                    timeCard.addToTimecard(punches.get(i));
                }
            }
        }
        if(right != null) right.GenerateGeneralManagerTimeCards(beginningOfWeek, endOfWeek);
    }

    /**
     * return the time card for an employee
     * @return
     */
    public Timecard getTimeCard()
    {
        return timeCard;
    }

    /**
     * adds a punch to punches
     * @param p
     */
    public void addPunch(Punch p)
    {
        punches.add(p);
    }

    /**
     * returns the last punch in the punches arrayList
     * @return
     */
    public Punch getLastPunch()
    {
        Punch temp = null;
        if(punches.size() == 0) temp = null;
        else temp = punches.get(punches.size()-1);
        return temp;
    }

    /**
     * calculates the pay for all employees at a location
     */
    public String calculatePay(long endOfWeek)
    {
        String temp = new String();
        double payForWeek = 0.0;
        double hoursForWeek = 0.0;
        double h = 0.0;
        double overTime = 0.0;

        if(left != null) temp += left.calculatePay(endOfWeek);

        if(timeCard.getApprovedStatus() == true)
        {
            ArrayList<Punch> tc = timeCard.getTimeCard();
            for(int i = 0; i < tc.size(); ++i)
            {
                h = tc.get(i).CalculateHours();
                if(h > 8.00)
                {
                    overTime += h-8.00;
                    hoursForWeek += 8.00;
                }
                else hoursForWeek += h;
            }
        }
        awardPTO((hoursForWeek+overTime), endOfWeek);
        hoursForWeek += checkForExpiredPTO(); //get rid of expired PTO and add the expired vacation time hours
        payForWeek += (hoursForWeek * payRate);
        payForWeek += (overTime * (payRate * 1.5));
        temp += "Employee ID: " + employeeID + ",Hours Worked: " + (hoursForWeek+overTime) + ",Payrate: " + payRate + ",Total Before Tax: " + payForWeek + "\n";

        if(right != null) temp += right.calculatePay(endOfWeek);

        return temp;
    }

    /**
     * awards the PTO to an employee based on the amount of hours worked in a week compared to the amount of hours need to work
     * in a week to recieve PTO
     * @param hoursForWeek amount of hours worked in a week
     * @param awardDate date PTO is awarded, used to calculate experation date
     */
    public void awardPTO(double hoursForWeek, long awardDate)
    {
        if(hoursForWeek >= minHoursForPTO)
        {
            addVacationTime(new VacationTime(vacationTimeIncrement, (long)(awardDate + (long)((long)2*31556952000L)))); //number of milliseconds in a year
            addSickTime((new SickTime(sickTimeIncrement, (long)(awardDate + 31556952000L))));
        }
        clearTimeCardAndPunches();
    }

    /**
     * checks all PTO in an employees PTO linked lists for any that have an expiration date past the current date
     */
    public int checkForExpiredPTO()
    {
        int hours = 0;
        SickTime prevSick;
        SickTime tempSick;
        VacationTime prevVaca;
        VacationTime tempVaca;
        long currTime = System.currentTimeMillis();
        if(sickTimeHead != null)
        {   
            while(sickTimeHead.getExpirationDate() <= currTime) sickTimeHead = sickTimeHead.getNext();
            if(sickTimeHead != null)
            {
                prevSick = sickTimeHead;
                tempSick = prevSick.getNext();
                while(tempSick != null)
                {
                    if(tempSick.getExpirationDate() <= currTime)
                    {
                        prevSick.setNext(tempSick.getNext());
                        tempSick = tempSick.getNext();
                    }
                    else
                    {
                        prevSick = tempSick;
                        tempSick = tempSick.getNext();
                    }
                }
            }
        }
        if(vacaTimeHead != null)
        {
            while(vacaTimeHead.getExpirationDate() <= currTime) vacaTimeHead = vacaTimeHead.getNext();
            if(vacaTimeHead != null)
            {
                prevVaca = vacaTimeHead;
                tempVaca = prevVaca.getNext();
                while(tempVaca != null)
                {
                    if(tempVaca.getExpirationDate() <= currTime)
                    {
                        hours += tempVaca.getHours();
                        prevVaca.setNext(tempVaca.getNext());
                        tempVaca = tempVaca.getNext();
                    }
                    else
                    {
                        prevVaca = tempVaca;
                        tempVaca = tempVaca.getNext();
                    }
                }
            }
        }
        return hours;
    }

    /**
     * deletes all punches that were in the timeCard object from the punches arrayList and then resets the time card
     */
    public void clearTimeCardAndPunches()
    {
        ArrayList<Punch> temp = timeCard.getTimeCard();
        for(int i = 0; i < temp.size(); ++i)
        {
            for(int j = 0; j < punches.size(); ++j)
            {
                if(temp.get(i).equals(punches.get(j))) punches.remove(j);
            }
        }
        timeCard = new Timecard();
    }

    /**
     * resets the timecards that a shift manager can approve. used when timecards are denied
     */
    public void resetTimecardForShiftManager()
    {
        if(left != null) left.resetTimecardForShiftManager();
        if(!position.equals("Shift Manager") && !position.equals("General Manager"))
        {
            timeCard = new Timecard();
        }
        if(right != null) right.resetTimecardForShiftManager();
    }

    /**
     * resets the timecards that a general manager can approve. used when the timecards are denied
     */
    public void resetTimecardForGeneralManager()
    {
        if(left != null) left.resetTimecardForGeneralManager();
        if(position.equals("Shift Manager") || position.equals("General Manager"))
        {
            timeCard = new Timecard();
        }
        if(right != null) right.resetTimecardForGeneralManager();
    }

    /**
     * creates the tree structure for employees at a location
     * @param e
     */
    public void insert(Employee e)
    {
        if(employeeID > e.getEmployeeID())
        {
            if(left == null) left = e;
            else left.insert(e);
        }
        else
        {
            if(right == null) right = e;
            else right.insert(e);
        }
    }
    
    /**
     * searches for an employee in a tree
     * @param id
     * @return
     */
    public Employee search(long id)
    {
        
        Employee temp = null;
        if(employeeID == id) temp = this;
        else if(employeeID != id && left == null && right == null) temp = null;
        else if(employeeID > id && left != null) temp = left.search(id);
        else if(employeeID < id && right != null) temp = right.search(id); 
        return temp;
    }

    /**
     * adds a sick time node to the sick time linked list
     */
    public void addSickTime(SickTime s)
    {
        if(sickTimeHead == null)
        {
            sickTimeHead = s;
            sickTimeTail = sickTimeHead;
        }
        else
        {
            sickTimeTail.setNext(s);
            sickTimeTail = sickTimeTail.getNext();
        }
    }

    /**
     * adds a vacation time node to the vacation time linked list
     * @param v
     */
    public void addVacationTime(VacationTime v)
    {
        if(vacaTimeHead == null)
        {
            vacaTimeHead = v;
            vacaTimeTail = vacaTimeHead;
        }
        else
        {
            vacaTimeTail.setNext(v);
            vacaTimeTail = vacaTimeTail.getNext();
        }
    }

    /**
     * prints the time cards a shift manager can approve
     */
    public void printTimeCardForShiftManager()
    {
        if(left != null) left.printTimeCardForShiftManager();
        if(!position.equals("Shift Manager") && !position.equals("General Manager")) //do nothing
        {
            System.out.print(this.toString());
            System.out.print(timeCard.toString());
        }
        if(right != null) right.printTimeCardForShiftManager();
    }

    /**
     * prints the timecards a general manager can approve
     */
    public void printTimeCardsForGeneralManager()
    {
        if(left != null) left.printTimeCardsForGeneralManager();
        if(position.equals("Shift Manager") || position.equals("General Manager"))
        {
            System.out.print(this.toString());
            System.out.print(timeCard.toString());
        }
        if(right != null) right.printTimeCardsForGeneralManager();
    }

    /**
     * sets the time cards a shift manager approves to be approved
     */
    public void approveShiftManagerTimeCard()
    {
        if(left != null) left.approveShiftManagerTimeCard();
        if(!position.equals("Shift Manager") && !position.equals("General Manager")) //do nothing
        {
            timeCard.setApproved(true);
        }
        if(right != null) right.approveShiftManagerTimeCard();
    }

    /**
     * sets the time cards a general manager can approve to be approved
     */
    public void approveGeneralManagerTimeCard()
    {
        if(left != null) left.approveGeneralManagerTimeCard();
        if(position.equals("Shift Manager") || position.equals("General Manager"))
        {
            timeCard.setApproved(true);
        }
        if(right != null) right.approveGeneralManagerTimeCard();
    }

    /**
     * prints all time cards. for testing purposes
     * @return
     */
    public String printTimeCard()
    {   
        String temp = new String();
        if(left != null) temp += left.printTimeCard();
        temp += this.toString();
        temp += timeCard.toString();
        if(right != null) temp += right.printTimeCard();
        return temp;
    }

    /**
     * check the shift manager time cards for missing punches.
     * @param punchMissing
     * @return
     */
    public boolean checkForMissingPunchesShiftManager(boolean punchMissing)
    {
        if(punchMissing == true) return punchMissing;
        if(left != null) punchMissing = left.checkForMissingPunchesShiftManager(punchMissing);
        if(!position.equals("Shift Manager") && !position.equals("General Manager"))
        {
            ArrayList<Punch> temp = timeCard.getTimeCard();
            if(temp.size() == 0); //do nothing
            else 
            {
                for(Punch p: temp)
                {
                    if(p.getInTime() == 0 || p.getOutTime() == 0) 
                    {
                        punchMissing = true;
                        break;
                    }
                }
            }
        }
        if(right != null) punchMissing = right.checkForMissingPunchesShiftManager(punchMissing);
        return punchMissing;
    }

    /**
     * checks the general manager time cards for missing punches
     */
    public boolean checkForMissingPunchesGeneralManager(boolean punchMissing)
    {
        if(punchMissing == true) return punchMissing;
        if(left != null) punchMissing = left.checkForMissingPunchesGeneralManager(punchMissing);
        if(position.equals("Shift Manager") || position.equals("General Manager"))
        {
            ArrayList<Punch> temp = timeCard.getTimeCard();
            if(temp.size() == 0); //do nothing
            else
            {
                for(Punch p: temp)
                {
                    if(p.getInTime() == 0 || p.getOutTime() == 0) 
                    {
                        punchMissing = true;
                        break;
                    }
                }
            }
        }
        if(right != null) punchMissing = right.checkForMissingPunchesGeneralManager(punchMissing);
        return punchMissing;
    }

    /**
     * finds a punch in the punches array list
     * @param date
     * @return
     */
    public Punch findPunch(long date)
    {
        Punch temp = null;
        for(Punch p : punches)
        {
            if(p.getDate() == date) 
            {
                temp = p;
                break;
            }
        }
        return temp;
    }
}


class Supervisor extends Employee
{
    /**
     * constructor for making a supervisor based on position
     * @param EID
     * @param fn
     * @param ln
     * @param p
     * @param l
     */
    public Supervisor(long EID, String fn, String ln, String p, Location l)
    {
        super(EID, fn, ln, p, l);
        try
        {
            if(p.trim().equals("Assistant Manager")) 
            {
                sickTimeIncrement = 2;
                vacationTimeIncrement = 3;
                minHoursForPTO = 5;
                setPayRate(25);
            }
            else if(p.trim().equals("Shift Manager"))
            {
                sickTimeIncrement = 2;
                vacationTimeIncrement = 4;
                minHoursForPTO = 8;
                setPayRate(30);
            }
            else if(p.trim().equals("General Manager"))
            {
                sickTimeIncrement = 4;
                vacationTimeIncrement = 8;
                minHoursForPTO = 38;
                setPayRate(35);
            }
            else
            {
                throw new UnknownPositionException("Supervisor position does not exist.");
            }
        }
        catch(UnknownPositionException e)
        {
            System.out.println("UnknownPositionException: " + e.getMessage());
        }
    }
}


class Timecard
{
    ArrayList<Punch> timeCard = new ArrayList<Punch>(7);
    boolean approved = false;

    /**
     * overriden toString() method to print a time card
     */
    public String toString()
    {
        String temp = new String();
        if(timeCard.size() == 0) temp += "No Punches for the Week.\n";
        else
        {
            for(int i = 0; i < timeCard.size(); ++i)
            {
                temp += timeCard.get(i).toString();
            }
        }      
        return temp;
    }

    /**
     * adds a punch to the timeCard arrayList
     */
    public void addToTimecard(Punch p)
    {
        timeCard.add(p);
    }

    /**
     * returns the approved variables value
     */
    public boolean getApprovedStatus()
    {
        return approved;
    }

    /**
     * returns the timeCard arrayList
     * @return
     */
    public ArrayList<Punch> getTimeCard()
    {
        return timeCard;
    }

    /**
     * sets the approved variable to p
     * @param p
     */
    public void setApproved(boolean p)
    {
        approved = p;
    }
}

class Punch
{
    long outTime = 0;
    long inTime = 0;
    long date;

    /**
     * constructor to create a punch
     * @param d date of punch
     * @param io true = in-punch, false = out-punch
     * @param h hour of punch
     */
    public Punch(long d, boolean io, long h)
    {
        date = d;
        if(io == true) inTime = h;
        else outTime = h;
    }
    
    /**
     * calcualtes how many hours a given punch has
     */
    public double CalculateHours()
    {
        double hoursWorked = 0;
        int wholeHours = (int)(((outTime - inTime)/1000)/60)/60;
        double remainderHours = (((outTime - inTime)/1000)/60)%60;
        if(remainderHours > 0.0 && remainderHours <= 15) remainderHours = .25;
        else if(remainderHours > 15 && remainderHours <= 30) remainderHours = .50;
        else if(remainderHours > 30 && remainderHours <= 45) remainderHours = .75;
        else if(remainderHours > 45 && remainderHours <= 60) remainderHours = 1.00;
        hoursWorked = wholeHours + remainderHours;
        return hoursWorked;
    }

    /**
     * sets the out time for a punch
     * @param o
     */
    public void setOutTime(long o)
    {
        outTime = o;
    }

    /**
     * sets the in time for a punch
     * @param i
     */
    public void setInTime(long i)
    {
        inTime = i;
    }

    /**
     * returns the out time for a punch
     * @return
     */
    public long getOutTime()
    {
        return outTime;
    }

    /**
     * returns the in time for a punch
     * @return
     */
    public long getInTime()
    {
        return inTime;
    }

    /**
     * returns the milliseconds version of the date
     */
    public long getDate()
    {
        return date;
    }

    /**
     * returns the string version of the date
     * @return
     */
    public String getFormattedDate()
    {
        Date temp = new Date(date);
        DateFormat d = new SimpleDateFormat("MM/dd/yyyy");
        String formattedDate = d.format(temp);
        return formattedDate;
    }

    /**
     * returns the string version of the in time
     * @return
     */
    public String getFormattedInTime()
    {
        if(inTime == 0) return new String("NO CLOCK IN");
        else
        {
            Date temp = new Date(inTime);
            DateFormat d = new SimpleDateFormat("HH:mm");
            String formattedInTime = d.format(temp);
            return formattedInTime;
        }
    }

    /**
     * returns the string version of the out time
     * @return
     */
    public String getFormattedOutTime()
    {
        if(outTime == 0) return new String("NO CLOCK OUT");
        else
        {
            Date temp = new Date(outTime);
            DateFormat d = new SimpleDateFormat("HH:mm");
            String formattedOutTime = d.format(temp);
            return formattedOutTime;
        }
    }

    /**
     * overridden toString() function for printing punches
     */
    public String toString()
    {
        return new String(getFormattedDate() + " " + getFormattedInTime() + " " + getFormattedOutTime() + "\n");
    }

    /**
     * overriden equals function to compare to punches
     */
    @Override
    public boolean equals(Object l)
    {
        boolean isEqual = false;
        if(l != null && l instanceof Punch)
        {
            isEqual = (this.date == ((Punch)l).getDate())?true:false;
        }
        return isEqual;
    }
}

abstract class PTO 
{
    protected int hours;
    protected long expirationDate;
    boolean used = false;
    String dateUsed;

    /**
     * creates a PTO for a day that has already past
     */
    public PTO(int h, String ed)
    {
        hours = h;
        dateUsed = ed;
    }

    /**
     * creates a generic PTO PTO for 
     * @param h
     * @param ed
     */
    public PTO(int h, long ed) 
    {
        hours = h;
        expirationDate = ed;
    }

    /**
     * creates a PTO without an expiration date
     * @param h
     */
    public PTO(int h)
    {
        hours = h;
    }

    /**
     * returns the amount of hours avaliable on this PTO node
     * @return
     */
    public int getHours()
    {
        return hours;
    }

    /**
     * sets the amount of hours on this PTO node
     * @param h
     */
    public void setHours(int h)
    {
        hours = h;
    }

    /**
     * returns the expiration date for this PTO node
     * @return
     */
    public long getExpirationDate()
    {
        return expirationDate;
    }

    /**
     * sets the used variable for this PTO node.
     * @param b
     */
    public void setUsed(boolean b)
    {
        used = b;
    }
    
    /**
     * sets the date this PTO was used
     * @param du
     */
    public void setDateUsed(String du)
    {
        dateUsed = du;
    }

    /**
     * return the date this PTO was used
     * @return
     */
    public String getDateUsed()
    {
        return dateUsed;
    }

    /**
     * reutrn the used variables status
     */
    public boolean getUsed()
    {
        return used;
    }

    /**
     * overriden toString() method for printing PTO
     */
    public String toString()
    {
        Date tempFullDate = new Date(expirationDate);
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        String strDate = dateFormat.format(tempFullDate);
        return new String(hours + " hours expires on " + strDate + "\n");
    }

    /**
     * used for printing used PTO, must be implemented by child classes
     * @return
     */
    abstract public String usedToString();

    /**
     * returns the next node for this PTO node, must be implemented by child classes
     * @return
     */
    abstract public PTO getNext();

    /**
     * sets the next node for this PTO node, must be implemented by child classes
     * @param p
     */
    abstract public void setNext(PTO p);

    /**
     * returns the string version of the expiration date
     * @return
     */
    public String getFormattedDate()
    {
        Date dateCreator = new Date(expirationDate);
        DateFormat formatForExpirationDate = new SimpleDateFormat("MM/dd/yyyy");
        String formattedDate = formatForExpirationDate.format(dateCreator);
        return formattedDate;
    }
}


class SickTime extends PTO
{
    SickTime next = null;
    long sickTimeExpiration = 31536000000L; //number of milliseconds in a year

    /**
     * default constructor for creating sickTime already used
     * @param hours
     * @param ed
     */
    public SickTime(int hours, String ed)
    {
        super(hours,ed);
    }

    /**
     * default constructor for creating sickTime
     * @param hours
     * @param ed
     */
    public SickTime(int hours, long ed)
    {
        super(hours, ed);
    }
    
    /**
     * creates sick time without expiration date
     * @param hours
     */
    public SickTime(int hours)
    {
        super(hours);
        expirationDate = System.currentTimeMillis() + sickTimeExpiration;
    }

    /**
     * returns the next node
     */
    public SickTime getNext()
    {
        return next;
    }

    /**
     * sets the next node
     */
    public void setNext(PTO s)
    {
        next = (SickTime)s; //casting to go from abstract PTO to SickTime
    }

    /**
     * returns a string of the used sick time
     */
    public String usedToString()
    {
        return new String("Used " + getHours() + " hours of Sick Time on " + getDateUsed() + "\n");
    }
}

class VacationTime extends PTO
{
    VacationTime next = null;
    long vacationTimeExpiration = (2 * 31536000000L);

    /**
     * defaults constructor for vacation time that has been used
     * @param hours
     * @param ed
     */
    public VacationTime(int hours, String ed)
    {
        super(hours,ed);
    }

    /**
     * defaults constructor for vacation time
     * @param hours
     * @param ed
     */
    public VacationTime(int hours, long ed)
    {
        super(hours, ed);
    }
    
    /**
     * constructor without expiration date
     * @param hours
     */
    public VacationTime(int hours)
    {
        super(hours);
        expirationDate = System.currentTimeMillis() + vacationTimeExpiration;
    }

    /**
     * returns next node
     */
    public VacationTime getNext()
    {
        return next;
    }

    /**
     * sets next node
     */
    public void setNext(PTO v)
    {
        next = (VacationTime)v; //casting to go from abstract PTO to VacationTime
    }

    /**
     * returns a string of the used vacation time
     */
    public String usedToString()
    {
        return new String("Used " + getHours() + " hours of Vacation Time on " + getDateUsed() + "\n");
    }
}

class Location
{
    String locationName;
    Employee generalManager = new Employee(); //root
    Employee root;

    /**
     * constructor to make a location
     * @param ln
     */
    public Location(String ln)
    {
        locationName = ln;
    }

    /**
     * constructor to make a location with a general manager
     * @param ln
     * @param gm
     */
    public Location(String ln, Employee gm)
    {
        locationName = ln;
        generalManager = gm;
    }
    
    /**
     * sets the general manager
     * @param gm
     */
    public void setGeneralManager(Employee gm)
    {
        generalManager = gm;
    }

    /**
     * returns the general manager
     * @return
     */
    public Employee getGeneralManager()
    {
        return generalManager;
    }

    /**
     * returns the location name
     */
    public String getLocationName()
    {
        return locationName;
    }

    /**
     * adds an employee to the tree via the root leaf
     * @param e
     */
    public void addEmployee(Employee e)
    {
        if(e.getPosition().equals("General Manager")) generalManager = e;
        if(root == null) root = e;
        else root.insert(e);
    }

    /**
     * searches an employee in the tree via the root leaf
     * @param id
     * @return
     */
    public Employee searchEmployees(long id)
    {
        Employee temp = root.search(id);
        return temp;
    }

    /**
     * overridden equals function that compares two location via location name 
     */
    @Override
    public boolean equals(Object l)
    {
        boolean isEqual = false;
        if(l != null && l instanceof Location)
        {
            isEqual = (this.locationName.equals(((Location)l).getLocationName()))?true:false;
        }
        return isEqual;
    }

    /**
     * prints a shift managers time cards at a location
     * @param beginningOfWeek
     * @param endOfWeek
     */
    public void printShiftManagerTimeCards(long beginningOfWeek, long endOfWeek)
    {
        root.GenerateShiftManagerTimeCards(beginningOfWeek, endOfWeek);
        System.out.println("Time cards for week:\n");
        root.printTimeCardForShiftManager();
    }

    /**
     * prints a general managers time cards at a location
     * @param beginningOfWeek
     * @param endOfWeek
     */
    public void printGeneralManagerTimeCards(long beginningOfWeek, long endOfWeek)
    {
        root.GenerateGeneralManagerTimeCards(beginningOfWeek, endOfWeek);
        System.out.println("Time cards for week:\n");
        root.printTimeCardsForGeneralManager();
    }

    /**
     * approves a shift managers timecards
     */
    public void approveShiftManagerTimeCards()
    {
        
        root.approveShiftManagerTimeCard();
    }

    /**
     * approves a general managers timecards
     */
    public void approveGeneralManagerTimeCards()
    {
        root.approveGeneralManagerTimeCard();
    }

    /**
     * gets the payroll for a week
     * @param endOfWeek
     * @return
     */
    public String runPayroll(long endOfWeek)
    {
        String temp = locationName + "\n";
        temp += root.calculatePay(endOfWeek);
        return temp;
    }

    /**
     * checks if shift manager timecards have missing punches
     * @return
     */
    public boolean checkShiftManagerPunches()
    {
        return root.checkForMissingPunchesShiftManager(false);
    }
    
    /**
     * checks if general manager timecards have missing punches
     * @return
     */
    public boolean checkGeneralManagerPunches()
    {
        return root.checkForMissingPunchesGeneralManager(false);
    }

    /**
     * resest a shift managers timecards if they are denied
     */
    public void resetShiftManagerTimeCards()
    {
        root.resetTimecardForShiftManager();
    }

    /**
     * resest a general managers timecards if they are denied
     */
    public void resetGeneralManagerTimeCards()
    {
        root.resetTimecardForGeneralManager();
    }

    /**
     * gets employee data in csv format
     * @return
     */
    public String getEmployeeData()
    {
        String temp = new String(root.getEmployeeDataAsCSV());
        return temp;
    }

    /**
     * gets time off data in csv format
     * @return
     */
    public String getTimeOffData()
    {
        String temp = new String(root.getTimeOffDataAsCSV());
        return temp;
    }

    /**
     * gets punch data in csv format
     */
    public String getPunchData()
    {
        String temp = new String(root.getPunchDataAsCSV());
        return temp;
    }
}