import java.io.*;
import java.util.*;

// **********************************************************************
// The ASTnode class defines the nodes of the abstract-syntax tree that
// represents a base program.
//
// Internal nodes of the tree contain pointers to children, organized
// either in a list (for nodes that may have a variable number of 
// children) or as a fixed set of fields.
//
// The nodes for literals and identifiers contain line and character 
// number information; for string literals and identifiers, they also 
// contain a string; for integer literals, they also contain an integer 
// value.
//
// Here are all the different kinds of AST nodes and what kinds of 
// children they have.  All of these kinds of AST nodes are subclasses
// of "ASTnode".  Indentation indicates further subclassing:
//
//     Subclass              Children
//     --------              --------
//     ProgramNode           DeclListNode
//     DeclListNode          linked list of DeclNode
//     DeclNode:
//       VarDeclNode         TypeNode, IdNode, int
//       FctnDeclNode        TypeNode, IdNode, FormalsListNode, FctnBodyNode
//       FormalDeclNode      TypeNode, IdNode
//       TupleDeclNode       IdNode, DeclListNode
//
//     StmtListNode          linked list of StmtNode
//     ExpListNode           linked list of ExpNode
//     FormalsListNode       linked list of FormalDeclNode
//     FctnBodyNode          DeclListNode, StmtListNode
//
//     TypeNode:
//       LogicalNode         --- none ---
//       IntegerNode         --- none ---
//       VoidNode            --- none ---
//       TupleNode           IdNode
//
//     StmtNode:
//       AssignStmtNode      AssignExpNode
//       PostIncStmtNode     ExpNode
//       PostDecStmtNode     ExpNode
//       IfStmtNode          ExpNode, DeclListNode, StmtListNode
//       IfElseStmtNode      ExpNode, DeclListNode, StmtListNode,
//                                    DeclListNode, StmtListNode
//       WhileStmtNode       ExpNode, DeclListNode, StmtListNode
//       ReadStmtNode        ExpNode
//       WriteStmtNode       ExpNode
//       CallStmtNode        CallExpNode
//       ReturnStmtNode      ExpNode
//
//     ExpNode:
//       TrueNode            --- none ---
//       FalseNode           --- none ---
//       IdNode              --- none ---
//       IntLitNode          --- none ---
//       StrLitNode          --- none ---
//       TupleAccessNode     ExpNode, IdNode
//       AssignExpNode       ExpNode, ExpNode
//       CallExpNode         IdNode, ExpListNode
//       UnaryExpNode        ExpNode
//         UnaryMinusNode
//         NotNode
//       BinaryExpNode       ExpNode ExpNode
//         PlusNode     
//         MinusNode
//         TimesNode
//         DivideNode
//         EqualsNode
//         NotEqualsNode
//         LessNode
//         LessEqNode
//         GreaterNode
//         GreaterEqNode
//         AndNode
//         OrNode
//
// Here are the different kinds of AST nodes again, organized according to
// whether they are leaves, internal nodes with linked lists of children, 
// or internal nodes with a fixed number of children:
//
// (1) Leaf nodes:
//        LogicalNode,  IntegerNode,  VoidNode,    IdNode,  
//        TrueNode,     FalseNode,    IntLitNode,  StrLitNode
//
// (2) Internal nodes with (possibly empty) linked lists of children:
//        DeclListNode, StmtListNode, ExpListNode, FormalsListNode
//
// (3) Internal nodes with fixed numbers of children:
//        ProgramNode,     VarDeclNode,     FctnDeclNode,  FormalDeclNode,
//        TupleDeclNode,   FctnBodyNode,    TupleNode,     AssignStmtNode,
//        PostIncStmtNode, PostDecStmtNode, IfStmtNode,    IfElseStmtNode,
//        WhileStmtNode,   ReadStmtNode,    WriteStmtNode, CallStmtNode,
//        ReturnStmtNode,  TupleAccessNode, AssignExpNode, CallExpNode,
//        UnaryExpNode,    UnaryMinusNode,  NotNode,       BinaryExpNode,   
//        PlusNode,        MinusNode,       TimesNode,     DivideNode,
//        EqualsNode,      NotEqualsNode,   LessNode,      LessEqNode,
//        GreaterNode,     GreaterEqNode,   AndNode,       OrNode
//
// **********************************************************************

// **********************************************************************
//   ASTnode class (base class for all other kinds of nodes)
// **********************************************************************

abstract class ASTnode { 
    // every subclass must provide an unparse operation
    abstract public void unparse(PrintWriter p, int indent);

    // this method can be used by the unparse methods to do indenting
    protected void doIndent(PrintWriter p, int indent) {
        for (int k=0; k<indent; k++) p.print(" ");
    }

    public static String fctnName;
    public static HashMap<String, String> stringMap;
}

// **********************************************************************
//   ProgramNode, DeclListNode, StmtListNode, ExpListNode, 
//   FormalsListNode, FctnBodyNode
// **********************************************************************

class ProgramNode extends ASTnode {
    public ProgramNode(DeclListNode L) {
        myDeclList = L;
    }

    /***
     * nameAnalysis
     * Creates an empty symbol table for the outermost scope, then processes
     * all of the globals, tuple defintions, and functions in the program.
     ***/
    public void nameAnalysis() {
        SymTable symTab = new SymTable();
        myDeclList.nameAnalysis(symTab);
        if (noMain) {
            ErrMsg.fatal(0, 0, "No main function");
        }
    }
	
    /***
     * typeCheck
     ***/
    public void typeCheck() {
        myDeclList.typeCheck();
    }
	
    /***
     * codeGen
     ***/
    public void codeGen() {
        stringMap = new HashMap<String, String>();
        fctnName = null;
        myDeclList.codeGen();
    }

    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
    }

    // 1 child
    private DeclListNode myDeclList;
    public static boolean noMain = true; 
}

class DeclListNode extends ASTnode {
    public DeclListNode(List<DeclNode> S) {
        myDecls = S;
    }

    public void codeGen() {
        for(DeclNode node : myDecls) {
            if(node instanceof FctnDeclNode || node instanceof VarDeclNode) {
                node.codeGen();
            }
        }
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, process all of the decls in the list.
     ***/
    public void nameAnalysis(SymTable symTab) {
        nameAnalysis(symTab, symTab);
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab and a global symbol table globalTab
     * (for processing tuple names in variable decls), process all of the 
     * decls in the list.
     ***/    
    public void nameAnalysis(SymTable symTab, SymTable globalTab) {
        for (DeclNode node : myDecls) {
            if (node instanceof VarDeclNode) {
                ((VarDeclNode)node).nameAnalysis(symTab, globalTab);
            } else {
                node.nameAnalysis(symTab);
            }
        }
    }

    /***
     * typeCheck
     ***/
    public void typeCheck() {
        for (DeclNode node : myDecls) {
            node.typeCheck();
        }
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator it = myDecls.iterator();
        try {
            while (it.hasNext()) {
                ((DeclNode)it.next()).unparse(p, indent);
            }
        } catch (NoSuchElementException ex) {
            System.err.println("unexpected NoSuchElementException in DeclListNode.print");
            System.exit(-1);
        }
    }

    // list of children (DeclNodes)
    private List<DeclNode> myDecls;
}

class StmtListNode extends ASTnode {
    public StmtListNode(List<StmtNode> S) {
        myStmts = S;
    }

    public void codeGen()  {
        for(StmtNode node : myStmts) {
            node.codeGen();
        }
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, process each statement in the list.
     ***/
    public void nameAnalysis(SymTable symTab) {
        for (StmtNode node : myStmts) {
            node.nameAnalysis(symTab);
        }
    } 
    
    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        for(StmtNode node : myStmts) {
            node.typeCheck(retType);
        }
    }
    
    public void unparse(PrintWriter p, int indent) {
        Iterator<StmtNode> it = myStmts.iterator();
        while (it.hasNext()) {
            it.next().unparse(p, indent);
        } 
    }

    // list of children (StmtNodes)
    private List<StmtNode> myStmts;
}

class ExpListNode extends ASTnode {
    public ExpListNode(List<ExpNode> S) {
        myExps = S;
    }

    public void codeGen() {
        for(ExpNode node : myExps) {
            node.codeGen();
        }
    }

    public int size() {
        return myExps.size();
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, process each exp in the list.
     ***/
    public void nameAnalysis(SymTable symTab) {
        for (ExpNode node : myExps) {
            node.nameAnalysis(symTab);
        }
    }
    
    /***
     * typeCheck    
     ***/
    public void typeCheck(List<Type> typeList) {
        int k = 0;
        try {
            for (ExpNode node : myExps) {
                Type actualType = node.typeCheck();     // actual type of arg
                
                if (!actualType.isErrorType()) {        // if this is not an error
                    Type formalType = typeList.get(k);  // get the formal type
                    if (!formalType.equals(actualType)) {
                        ErrMsg.fatal(node.lineNum(), node.charNum(),
                                     "Actual type does not match formal type");
                    }
                }
                k++;
            }
        } catch (NoSuchElementException e) {
            System.err.println("unexpected NoSuchElementException in ExpListNode.typeCheck");
            System.exit(-1);
        }
    }
    
    public void unparse(PrintWriter p, int indent) {
        Iterator<ExpNode> it = myExps.iterator();
        if (it.hasNext()) {         // if there is at least one element
            it.next().unparse(p, indent);
            while (it.hasNext()) {  // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent);
            }
        } 
    }

    // list of children (ExpNodes)
    private List<ExpNode> myExps;
}
class FormalsListNode extends ASTnode {
    public FormalsListNode(List<FormalDeclNode> S) {
        myFormals = S;
    }

    //No Code Gen needed for formals ->

    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * for each formal decl in the list
     *     process the formal decl
     *     if there was no error, add type of formal decl to list
     ***/
    public List<Type> nameAnalysis(SymTable symTab) {
        List<Type> typeList = new LinkedList<Type>();
        for (FormalDeclNode node : myFormals) {
            Sym sym = node.nameAnalysis(symTab);
            if (sym != null) {
                typeList.add(sym.getType());
            }
        }
        return typeList;
    }    
    
    /***
     * Return the number of formals in this list.
     ***/
    public int length() {
        return myFormals.size();
    }

    public void unparse(PrintWriter p, int indent) {
        Iterator<FormalDeclNode> it = myFormals.iterator();
        if (it.hasNext()) { // if there is at least one element
            it.next().unparse(p, indent);
            while (it.hasNext()) {  // print the rest of the list
                p.print(", ");
                it.next().unparse(p, indent);
            }
        }
    }

    // list of children (FormalDeclNodes)
    private List<FormalDeclNode> myFormals;
}

class FctnBodyNode extends ASTnode {
    public FctnBodyNode(DeclListNode declList, StmtListNode stmtList) {
        myDeclList = declList;
        myStmtList = stmtList;
    }

    public void codeGen() {
        myStmtList.codeGen();
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - process the declaration list
     * - process the statement list
     ***/
    public void nameAnalysis(SymTable symTab) {
        myDeclList.nameAnalysis(symTab);
        myStmtList.nameAnalysis(symTab);
    }
 
    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        myStmtList.typeCheck(retType);
    }    
    
    public void unparse(PrintWriter p, int indent) {
        myDeclList.unparse(p, indent);
        myStmtList.unparse(p, indent);
    }

    // 2 children
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}


// **********************************************************************
// ****  DeclNode and its subclasses
// **********************************************************************

abstract class DeclNode extends ASTnode {
    /***
     * Note: a formal decl needs to return a sym
     ***/
    abstract public Sym nameAnalysis(SymTable symTab);

    // default version of typeCheck for non-function decls
    public void typeCheck() { }

    //default version of codeGen for iterating through the declnodes
    public void codeGen() { }
}

class VarDeclNode extends DeclNode {
    public VarDeclNode(TypeNode type, IdNode id, int size) {
        myType = type;
        myId = id;
        mySize = size;
    }

    public void codeGen() {
        //Q2 -> How to tell if a sym is global?
        if(myId.sym().isGlobal()) {
            Codegen.generate(".data");
            Codegen.generate(".align 2");
            Codegen.generateLabeled(("_" + myId.name()), ".space " + mySize, "Variable Declaration");
        } else {
            Codegen.generateLabeled(("_" + myId.name()), ".space " + mySize, "Variable Declaration");
        }

    }

    /***
     * nameAnalysis (overloaded)
     * Given a symbol table symTab, do:
     * if this name is declared void, then error
     * else if the declaration is of a tuple type, 
     *     lookup type name (globally)
     *     if type name doesn't exist, then error
     * if no errors so far,
     *     if name has already been declared in this scope, then error
     *     else add name to local symbol table     
     *
     * symTab is local symbol table (say, for tuple field decls)
     * globalTab is global symbol table (for tuple type names)
     * symTab and globalTab can be the same
     ***/
    public Sym nameAnalysis(SymTable symTab) {
        return nameAnalysis(symTab, symTab);
    }
    
    public Sym nameAnalysis(SymTable symTab, SymTable globalTab) {
        boolean badDecl = false;
        String name = myId.name();
        Sym sym = null;
        IdNode tupleId = null;

        if (myType instanceof VoidNode) {  // check for void type
            ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
                         "Non-function declared void");
            badDecl = true;        
        }
        
        else if (myType instanceof TupleNode) {
            tupleId = ((TupleNode)myType).idNode();
			try {
				sym = globalTab.lookupGlobal(tupleId.name());
            
				// if the name for the tuple type is not found, 
				// or is not a tuple type
				if (sym == null || !(sym instanceof TupleDefSym)) {
					ErrMsg.fatal(tupleId.lineNum(), tupleId.charNum(), 
								"Invalid name of tuple type");
					badDecl = true;
				}
				else {
					tupleId.link(sym);
				}
			} catch (EmptySymTableException ex) {
				System.err.println("Unexpected EmptySymTableException " +
								    " in VarDeclNode.nameAnalysis");
				System.exit(-1);
			} 
        }
        
		try {
			if (symTab.lookupLocal(name) != null) {
				ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
							"Multiply-declared identifier");
				badDecl = true;            
			}
		} catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in VarDeclNode.nameAnalysis");
            System.exit(-1);
        } 
        
        if (!badDecl) {  // insert into symbol table
            try {
                if (myType instanceof TupleNode) {
                    sym = new TupleSym(tupleId);        
                }
                else {
                    sym = new Sym(myType.type());
                    if (!globalTab.isGlobalScope()) {
                        int offset = globalTab.getOffset();
                        sym.setOffset(offset);
                        globalTab.setOffset(offset - 4); // vars are integer or logical
                    } else {
                            sym.setOffset(1);
                    }
                }
                symTab.addDecl(name, sym);
                myId.link(sym);
            } catch (DuplicateSymNameException ex) {
                System.err.println("Unexpected DuplicateSymNameException " +
                                   " in VarDeclNode.nameAnalysis");
                System.exit(-1);
            } catch (EmptySymTableException ex) {
                System.err.println("Unexpected EmptySymTableException " +
                                   " in VarDeclNode.nameAnalysis");
                System.exit(-1);
            }
        }
        
        return sym;
    } 

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
        p.println(".");
    }

    // 3 children
    private TypeNode myType;
    private IdNode myId;
    private int mySize;  // use value NON_TUPLE if this is not a tuple type

    public static int NON_TUPLE = -1;
}

//Hard One -> Double Check with TA
class FctnDeclNode extends DeclNode {
    public FctnDeclNode(TypeNode type,
                      IdNode id,
                      FormalsListNode formalList,
                      FctnBodyNode body) {
        myType = type;
        myId = id;
        myFormalsList = formalList;
        myBody = body;
    }

    public void codeGen() {
        fctnName = this.myId.name();
        Boolean isMain = false;

        if(fctnName.equals("main")) {
            Codegen.generate(".text");
            Codegen.generate(".globl main");
            Codegen.genLabel("main");
            isMain = true;
        } else {
            Codegen.generate(".text");
            Codegen.genLabel("_" + fctnName);
        }
        //Push RA
        Codegen.genPush(Codegen.RA);
        //Push Control Link
        Codegen.genPush(Codegen.FP);
        //Set the FP
        Codegen.p.println("#This is Setting the FP");
        Codegen.generate("addu", "$fp", "$sp", 8);
        //Pushing space for locals
        Codegen.p.println("#This is pushing space for locals");
        Codegen.generate("subu", "$sp", "$sp", ((this.myId).localsSize()));
        //Entering the function, coding the body
        myBody.codeGen();
        //Now, we exit:
        // Codegen.p.print("EXITING FUNCTION " + fctnName);
        //Code Exiting
        Codegen.p.println("#Loading the return address");
        Codegen.generateIndexed("lw", "$ra", "$fp", 0);
        //FP holds the Address to which need to restore SP
        Codegen.generate("move", Codegen.T0, "$fp");
        //Restoring FP
        Codegen.p.println("#Restoring FP");
        Codegen.generateIndexed("lw", "$fp", "$fp", -4);
        //Restoring SP
        Codegen.p.println("#Restoring SP");
        Codegen.generate("move", "$sp", Codegen.T0);
        //Return 
        if(isMain) {
            Codegen.generate("li", Codegen.V0, 10);
            Codegen.generate("syscall");
        } else {
            Codegen.generate("jr", "$ra");
        }
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * if this name has already been declared in this scope, then error
     * else add name to local symbol table
     * in any case, do the following:
     *     enter new scope
     *     process the formals
     *     if this function is not multiply declared,
     *         update symbol table entry with types of formals
     *     process the body of the function
     *     exit scope
     ***/
    public Sym nameAnalysis(SymTable symTab) {
        String name = myId.name();
        FctnSym sym = null;
        try {
			if (symTab.lookupLocal(name) != null) {
				ErrMsg.fatal(myId.lineNum(), myId.charNum(),
							"Multiply-declared identifier");
			}
        
			else { // add function name to local symbol table

                if (name.equals("main")) {
                    ProgramNode.noMain = false; 
                }

				try {
					sym = new FctnSym(myType.type(), myFormalsList.length());
					symTab.addDecl(name, sym);
					myId.link(sym);
				} catch (DuplicateSymNameException ex) {
					System.err.println("Unexpected DuplicateSymNameException " +
									" in FctnDeclNode.nameAnalysis");
					System.exit(-1);
				} catch (EmptySymTableException ex) {
					System.err.println("Unexpected EmptySymTableException " +
									" in FctnDeclNode.nameAnalysis");
					System.exit(-1);
				}
			}
		} catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in FctnDeclNode.nameAnalysis");
            System.exit(-1);
        } 
 
        symTab.setGlobalScope(false);
        symTab.setOffset(4);  // offset of first param  
        symTab.addScope();  // add a new scope for locals and params
        
        // process the formals
        List<Type> typeList = myFormalsList.nameAnalysis(symTab);
        if (sym != null) {
            sym.addFormals(typeList);
            sym.setParamsSize(symTab.getOffset() - 4);
        }

        symTab.setOffset(-8);  // offset of first local
        int temp = symTab.getOffset();

        myBody.nameAnalysis(symTab); // process the function body
       
        if (sym != null) {
            sym.setLocalsSize(-1*(symTab.getOffset() - temp));
        }
        symTab.setGlobalScope(true);

        try {
            symTab.removeScope();  // exit scope
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in FctnDeclNode.nameAnalysis");
            System.exit(-1);
        }
        
        return null;
    } 

    /***
     * typeCheck
     ***/
    public void typeCheck() {
        myBody.typeCheck(myType.type());
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
        p.print("{");
        myFormalsList.unparse(p, 0);
        p.println("} [");
        myBody.unparse(p, indent+4);
        p.println("]\n");
    }

    // 4 children
    private TypeNode myType;
    private IdNode myId;
    private FormalsListNode myFormalsList;
    private FctnBodyNode myBody;
}

class FormalDeclNode extends DeclNode {
    public FormalDeclNode(TypeNode type, IdNode id) {
        myType = type;
        myId = id;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * if this formal is declared void, then error
     * else if this formal is already in the local symble table,
     *     then issue multiply declared error message and return null
     * else add a new entry to the symbol table and return that Sym
     ***/
    public Sym nameAnalysis(SymTable symTab) {
        String name = myId.name();
        boolean badDecl = false;
        Sym sym = null;
        
        if (myType instanceof VoidNode) {
            ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
                         "Non-function declared void");
            badDecl = true;        
        }
        
        try { 
			if (symTab.lookupLocal(name) != null) {
				ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
							"Multiply-declared identifier");
				badDecl = true;
			}
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in FormalDeclNode.nameAnalysis");
            System.exit(-1);
        } 
        
        if (!badDecl) {  // insert into symbol table
            try {
                int offset = symTab.getOffset();
                sym = new Sym(myType.type());
                sym.setOffset(offset);
                symTab.setOffset(offset + 4); // only integer and logical formals
                symTab.addDecl(name, sym);
                myId.link(sym);
            } catch (DuplicateSymNameException ex) {
                System.err.println("Unexpected DuplicateSymNameException " +
                                   " in FormalDeclNode.nameAnalysis");
                System.exit(-1);
            } catch (EmptySymTableException ex) {
                System.err.println("Unexpected EmptySymTableException " +
                                   " in FormalDeclNode.nameAnalysis");
                System.exit(-1);
            }
        }
        
        return sym;
    }  

    public void unparse(PrintWriter p, int indent) {
        myType.unparse(p, 0);
        p.print(" ");
        myId.unparse(p, 0);
    }

    // 2 children
    private TypeNode myType;
    private IdNode myId;
}

class TupleDeclNode extends DeclNode {
    public TupleDeclNode(IdNode id, DeclListNode declList) {
        myId = id;
		myDeclList = declList;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * if this name is already in the symbol table,
     *     then multiply declared error (don't add to symbol table)
     * create a new symbol table for this tuple definition
     * process the decl list
     * if no errors
     *     add a new entry to symbol table for this tuple
     ***/
    public Sym nameAnalysis(SymTable symTab) {
        String name = myId.name();
        boolean badDecl = false;
        try {
			if (symTab.lookupLocal(name) != null) {
				ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
							"Multiply-declared identifier");
				badDecl = true;            
			}
		} catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in TupleDeclNode.nameAnalysis");
            System.exit(-1);
        } 

        SymTable tupleSymTab = new SymTable();
        
        // process the fields of the tuple
        myDeclList.nameAnalysis(tupleSymTab, symTab);
        
        if (!badDecl) {
            try {   // add entry to symbol table
                TupleDefSym sym = new TupleDefSym(tupleSymTab);
                symTab.addDecl(name, sym);
                myId.link(sym);
            } catch (DuplicateSymNameException ex) {
                System.err.println("Unexpected DuplicateSymNameException " +
                                   " in TupleDeclNode.nameAnalysis");
                System.exit(-1);
            } catch (EmptySymTableException ex) {
                System.err.println("Unexpected EmptySymTableException " +
                                   " in TupleDeclNode.nameAnalysis");
                System.exit(-1);
            }
        }
        
        return null;
    } 

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("tuple ");
        myId.unparse(p, 0);
        p.println(" {");
        myDeclList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("}.\n");
    }

    // 2 children
    private IdNode myId;
	private DeclListNode myDeclList;
}

// **********************************************************************
// *****  TypeNode and its subclasses
// **********************************************************************

abstract class TypeNode extends ASTnode {
    /* all subclasses must provide a type method */
    abstract public Type type();
}

class LogicalNode extends TypeNode {
    public LogicalNode() {
    }

    /***
     * type
     ***/
    public Type type() {
        return new LogicalType();
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("logical");
    }
}

class IntegerNode extends TypeNode {
    public IntegerNode() {
    }

    /***
     * type
     ***/
    public Type type() {
        return new IntegerType();
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("integer");
    }
}

class VoidNode extends TypeNode {
    public VoidNode() {
    }
    
    /***
     * type
     ***/
    public Type type() {
        return new VoidType();
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("void");
    }
}

class TupleNode extends TypeNode {
    public TupleNode(IdNode id) {
		myId = id;
    }
 
    public IdNode idNode() {
        return myId;
    }
       
    /***
     * type
     ***/
    public Type type() {
        return new TupleType(myId);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("tuple ");
        p.print(myId.name());
    }
	
	// 1 child
    private IdNode myId;
}

// **********************************************************************
// ****  StmtNode and its subclasses
// **********************************************************************

abstract class StmtNode extends ASTnode {
    abstract public void nameAnalysis(SymTable symTab);
    abstract public void typeCheck(Type retType);
    abstract public void codeGen();
}

class AssignStmtNode extends StmtNode {
    public AssignStmtNode(AssignExpNode assign) {
        myAssign = assign;
    }

    public void codeGen() {
        myAssign.codeGen();
        Codegen.genPop(Codegen.T0);
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     ***/
    public void nameAnalysis(SymTable symTab) {
        myAssign.nameAnalysis(symTab);
    }

    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        myAssign.typeCheck();
    }

    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myAssign.unparse(p, -1); // no parentheses
        p.println(".");
    }

    // 1 child
    private AssignExpNode myAssign;
}

class PostIncStmtNode extends StmtNode {
    public PostIncStmtNode(ExpNode exp) {
        myExp = exp;
    }
    
    public void codeGen() {
        myExp.codeGen();
        ((IdNode)myExp).genAddr();
        Codegen.genPop(Codegen.T0);
        Codegen.genPop(Codegen.T1);
        Codegen.generate("add", Codegen.T1, Codegen.T1, 1);
        Codegen.generateIndexed("sw", Codegen.T1, Codegen.T0, 0);
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     ***/
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }
    
    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck();
        
        if (!type.isErrorType() && !type.isIntegerType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Arithmetic operator used with non-integer operand");
        }
    }
    
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, 0);
        p.println("++.");
    }

    // 1 child
    private ExpNode myExp;
}

class PostDecStmtNode extends StmtNode {
    public PostDecStmtNode(ExpNode exp) {
        myExp = exp;
    }

    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     ***/
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }

    public void codeGen() {
        myExp.codeGen();
        ((IdNode)myExp).genAddr();
        Codegen.genPop(Codegen.T0);
        Codegen.genPop(Codegen.T1);
        Codegen.generate("add", Codegen.T1, Codegen.T1, -1);
        Codegen.generateIndexed("sw", Codegen.T1, Codegen.T0, 0);
    }
    
    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck();
        
        if (!type.isErrorType() && !type.isIntegerType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Arithmetic operator used with non-integer operand");
        }
    }
       
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myExp.unparse(p, 0);
        p.println("--.");
    }

    // 1 child
    private ExpNode myExp;
}

class IfStmtNode extends StmtNode {
    public IfStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myDeclList = dlist;
        myExp = exp;
        myStmtList = slist;
    }
    
    public void codeGen() {
        String doneLab = Codegen.nextLabel();   
        myExp.codeGen();
        Codegen.genPop(Codegen.T0);
        Codegen.generate("li", Codegen.T1, "0");
        Codegen.generate("beq", Codegen.T1, Codegen.T0, doneLab);
        myStmtList.codeGen();
        Codegen.genLabel(doneLab);
     }

    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - process the condition
     * - enter a new scope
     * - process the decls and stmts
     * - exit the scope
     ***/
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
        symTab.addScope();
        myDeclList.nameAnalysis(symTab);
        myStmtList.nameAnalysis(symTab);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in IfStmtNode.nameAnalysis");
            System.exit(-1);        
        }
    }
    
     /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck();
        
        if (!type.isErrorType() && !type.isLogicalType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Non-logical expression used in if condition");        
        }
        
        myStmtList.typeCheck(retType);
    }
           
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("if ");
        myExp.unparse(p, 0);
        p.println(" [");
        myDeclList.unparse(p, indent+4);
        myStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("]");  
    }

    // 3 children
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class IfElseStmtNode extends StmtNode {
    public IfElseStmtNode(ExpNode exp, DeclListNode dlist1,
                          StmtListNode slist1, DeclListNode dlist2,
                          StmtListNode slist2) {
        myExp = exp;
        myThenDeclList = dlist1;
        myThenStmtList = slist1;
        myElseDeclList = dlist2;
        myElseStmtList = slist2;
    }

    public void codeGen() {
        String false_label = Codegen.nextLabel();
        String end_label = Codegen.nextLabel();
        myExp.codeGen();
        Codegen.generate("li", Codegen.T1, "0");
        Codegen.generate("beq", Codegen.T0, Codegen.T1, false_label);
        myThenStmtList.codeGen();
        Codegen.generate("b", end_label);
        Codegen.genLabel(false_label);
        myElseStmtList.codeGen();
        Codegen.genLabel(end_label);
     }  
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - process the condition
     * - enter a new scope
     * - process the decls and stmts of then
     * - exit the scope
     * - enter a new scope
     * - process the decls and stmts of else
     * - exit the scope
     ***/
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
        symTab.addScope();
        myThenDeclList.nameAnalysis(symTab);
        myThenStmtList.nameAnalysis(symTab);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in IfStmtNode.nameAnalysis");
            System.exit(-1);        
        }
        symTab.addScope();
        myElseDeclList.nameAnalysis(symTab);
        myElseStmtList.nameAnalysis(symTab);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in IfStmtNode.nameAnalysis");
            System.exit(-1);        
        }
    }
    
    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck();
        
        if (!type.isErrorType() && !type.isLogicalType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Non-logical expression used in if condition");        
        }
        
        myThenStmtList.typeCheck(retType);
        myElseStmtList.typeCheck(retType);
    }
        
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("if ");
        myExp.unparse(p, 0);
        p.println(" [");
        myThenDeclList.unparse(p, indent+4);
        myThenStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("]");
        doIndent(p, indent);
        p.println("else [");
        myElseDeclList.unparse(p, indent+4);
        myElseStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("]"); 
    }

    // 5 children
    private ExpNode myExp;
    private DeclListNode myThenDeclList;
    private StmtListNode myThenStmtList;
    private StmtListNode myElseStmtList;
    private DeclListNode myElseDeclList;
}

class WhileStmtNode extends StmtNode {
    public WhileStmtNode(ExpNode exp, DeclListNode dlist, StmtListNode slist) {
        myExp = exp;
        myDeclList = dlist;
        myStmtList = slist;
    }

    public void codeGen() {
        String beginWhile = Codegen.nextLabel();
        String endWhile = Codegen.nextLabel();
        Codegen.genLabel(beginWhile, "begin while");
        myExp.codeGen();
        Codegen.genPop(Codegen.T0);
        Codegen.generate("beq", Codegen.T0, "0", endWhile);
        myStmtList.codeGen();
        Codegen.generate("b", beginWhile);
        Codegen.genLabel(endWhile, "end while");
     }

    
    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - process the condition
     * - enter a new scope
     * - process the decls and stmts
     * - exit the scope
     ***/
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
        symTab.addScope();
        myDeclList.nameAnalysis(symTab);
        myStmtList.nameAnalysis(symTab);
        try {
            symTab.removeScope();
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in IfStmtNode.nameAnalysis");
            System.exit(-1);        
        }
    }
    
    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck();
        
        if (!type.isErrorType() && !type.isLogicalType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Non-logical expression used in while condition");        
        }
        
        myStmtList.typeCheck(retType);
    }
    
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("while ");
        myExp.unparse(p, 0);
        p.println(" [");
        myDeclList.unparse(p, indent+4);
        myStmtList.unparse(p, indent+4);
        doIndent(p, indent);
        p.println("]");
    }

    // 3 children
    private ExpNode myExp;
    private DeclListNode myDeclList;
    private StmtListNode myStmtList;
}

class ReadStmtNode extends StmtNode {
    public ReadStmtNode(ExpNode e) {
        myExp = e;
    }

    public void codeGen() {
        ((IdNode)myExp).genAddr();
        Codegen.genPop(Codegen.T0);

        Codegen.generate("li", Codegen.V0, 5);
        Codegen.generate("syscall");
        Codegen.generateIndexed("sw", Codegen.V0, Codegen.T0, 0);
        

        
   }
    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     ***/
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    } 
 
    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck();
        
        if (type.isFctnType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Read attempt of function name");
        }
        
        if (type.isTupleDefType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Read attempt of tuple name");
        }
        
        if (type.isTupleType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Read attempt of tuple variable");
        }
    }
      
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("read >> ");
        myExp.unparse(p, 0);
        p.println(".");
    }

    // 1 child (actually can only be an IdNode or a TupleAccessNode)
    private ExpNode myExp;
}

class WriteStmtNode extends StmtNode {
    public WriteStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void codeGen() {
        myExp.codeGen();
        if(myType.isIntegerType()){
            Codegen.genPop(Codegen.A0);
            Codegen.generate("li", Codegen.V0, 1);
            Codegen.generate("syscall");
        }   
        if (myType.isStringType()) {
            Codegen.p.println("#WRITE STRING");
            Codegen.genPop(Codegen.A0);
            Codegen.generate("li", Codegen.V0, 4);
            Codegen.generate("syscall");
        } 
   }
    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     ***/
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }

    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        Type type = myExp.typeCheck();
        myType = type;
        
        if (type.isFctnType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Write attempt of function name");
        }
        
        if (type.isTupleDefType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Write attempt of tuple name");
        }
        
        if (type.isTupleType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Write attempt of tuple variable");
        }
        
        if (type.isVoidType()) {
            ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                         "Write attempt of void");
        }
    }
         
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("write << ");
        myExp.unparse(p, 0);
        p.println(".");
    }

    // 2 children
    private ExpNode myExp;
    private Type myType;
}

class CallStmtNode extends StmtNode {
    public CallStmtNode(CallExpNode call) {
        myCall = call;
    }

    public void codeGen() {
        myCall.codeGen();
        Codegen.genPop(Codegen.V0);
    }
    
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     ***/
    public void nameAnalysis(SymTable symTab) {
        myCall.nameAnalysis(symTab);
    }
    
    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        myCall.typeCheck();
    }
    
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        myCall.unparse(p, indent);
        p.println(".");
    }

    // 1 child
    private CallExpNode myCall;
}

class ReturnStmtNode extends StmtNode {
    public ReturnStmtNode(ExpNode exp) {
        myExp = exp;
    }

    public void codeGen() {
        if(myExp != null) {
            myExp.codeGen();
            Codegen.genPop(Codegen.V0);
        }
        Codegen.generateIndexed("lw", "$ra", "$fp", 0);
        //FP holds the Address to which need to restore SP
        Codegen.generate("move", Codegen.T0, "$fp");
        //Restoring FP
        Codegen.p.println("#Restoring FP");
        Codegen.generateIndexed("lw", "$fp", "$fp", -4);
        //Restoring SP
        Codegen.p.println("#Restoring SP");
        Codegen.generate("move", "$sp", Codegen.T0);
        //Return 
        Codegen.generate("jr", "$ra");
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child,
     * if it has one
     ***/
    public void nameAnalysis(SymTable symTab) {
        if (myExp != null) {
            myExp.nameAnalysis(symTab);
        }
    }

    /***
     * typeCheck
     ***/
    public void typeCheck(Type retType) {
        if (myExp != null) {  // return value given
            Type type = myExp.typeCheck();
            
            if (retType.isVoidType()) {
                ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                             "Return with value in void function");                
            }
            
            else if (!retType.isErrorType() && !type.isErrorType() && !retType.equals(type)){
                ErrMsg.fatal(myExp.lineNum(), myExp.charNum(),
                             "Return value wrong type");
            }
        }
        
        else {  // no return value given -- ok if this is a void function
            if (!retType.isVoidType()) {
                ErrMsg.fatal(0, 0, "Return value missing");                
            }
        }
        
    }
    
    public void unparse(PrintWriter p, int indent) {
        doIndent(p, indent);
        p.print("return");
        if (myExp != null) {
            p.print(" ");
            myExp.unparse(p, 0);
        }
        p.println(".");
    }

    // 1 child
    private ExpNode myExp; // possibly null
    private String currFunc;
}

// **********************************************************************
// ****  ExpNode and its subclasses
// **********************************************************************

abstract class ExpNode extends ASTnode {
    /***
     * Default version for nodes with no names
     ***/
    public void nameAnalysis(SymTable symTab) { }
        
    abstract public Type typeCheck();
    abstract public int lineNum();
    abstract public int charNum();
    public void codeGen() { }
    public void genJumpCode(String trueLabel, String falseLabel) {}
}

class TrueNode extends ExpNode {
    public TrueNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    public void codeGen() {
        Codegen.generate("li", Codegen.T0, Codegen.TRUE);
        Codegen.genPush(Codegen.T0);
    }

    public void genJumpCode(String trueLabel, String falseLabel) {
        this.codeGen();
        Codegen.generate("b", trueLabel);
    }

    /***
     * Return the line number for this literal.
     ***/
    public int lineNum() {
        return myLineNum;
    }
    
    /***
     * Return the char number for this literal.
     ***/
    public int charNum() {
        return myCharNum;
    }
    
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        return new LogicalType();
    }
     
    public void unparse(PrintWriter p, int indent) {
        p.print("True");
    }

    private int myLineNum;
    private int myCharNum;
}

class FalseNode extends ExpNode {
    public FalseNode(int lineNum, int charNum) {
        myLineNum = lineNum;
        myCharNum = charNum;
    }

    public void genJumpCode(String trueLabel, String falseLabel) {
        this.codeGen();
        Codegen.generate("b", falseLabel);
    }

    public void codeGen() {
        Codegen.generate("li", Codegen.T0, Codegen.FALSE);
        Codegen.genPush(Codegen.T0);
    }

    /***
     * Return the line number for this literal.
     ***/
    public int lineNum() {
        return myLineNum;
    }
    
    /***
     * Return the char number for this literal.
     ***/
    public int charNum() {
        return myCharNum;
    }

    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        return new LogicalType();
    }
        
    public void unparse(PrintWriter p, int indent) {
        p.print("False");
    }

    private int myLineNum;
    private int myCharNum;
}

class IdNode extends ExpNode {
    public IdNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }

    public void codeGenJumpAndLink() {
        Codegen.generate("jal", "_" + myStrVal);
    }

    public void codeGen() {
        if(mySym.isGlobal()) {
            Codegen.generate("lw", Codegen.T0, "_" + myStrVal);
        } else {
            Codegen.generateIndexed("lw", Codegen.T0, Codegen.FP, mySym.getOffset());
        }
        Codegen.genPush(Codegen.T0);
    }

    public void genAddr() {
        if(mySym.isGlobal()) {
            Codegen.generate("la", Codegen.T0, "_" + myStrVal);
        } else {
            Codegen.p.println("#Generating Indexed");
            Codegen.generateIndexed("la", Codegen.T0, Codegen.FP, mySym.getOffset());
        }
        Codegen.genPush(Codegen.T0);
    }

    /***
     * Link the given symbol to this ID.
     ***/
    public void link(Sym sym) {
        mySym = sym;
    }
    
    /***
     * Return the name of this ID.
     ***/
    public String name() {
        return myStrVal;
    }
    
    /***
     * Return the symbol associated with this ID.
     ***/
    public Sym sym() {
        return mySym;
    }
    
    /***
     * Return the line number for this ID.
     ***/
    public int lineNum() {
        return myLineNum;
    }
    
    /***
     * Return the char number for this ID.
     ***/
    public int charNum() {
        return myCharNum;
    }    
        
    /***
     * Return the total number of bytes for all local variables.
     * HINT: This method may be useful during code generation.
     ***/
    public int localsSize() {
        if(!(mySym instanceof FctnSym)) {
            throw new IllegalStateException("cannot call local size on a non-function");
        }
        return ((FctnSym)mySym).getLocalsSize();
    }    

    /***
     * Return the total number of bytes for all parameters.
     * HINT: This method may be useful during code generation.
     ***/
    public int paramsSize() {
        if(!(mySym instanceof FctnSym)) {
            throw new IllegalStateException("cannot call local size on a non-function");
        }
        return ((FctnSym)mySym).getParamsSize();
    }   

    /***
     * Is this function main?
     * HINT: This may be useful during code generation.
     ***/
    public boolean isMain() {
        return (myStrVal.equals("main"));
    } 

    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - check for use of undeclared name
     * - if ok, link to symbol table entry
     ***/
    public void nameAnalysis(SymTable symTab) {
		try {
            Sym sym = symTab.lookupGlobal(myStrVal);
            if (sym == null) {
                ErrMsg.fatal(myLineNum, myCharNum, "Undeclared identifier");
            } else {
                link(sym);
            }
        } catch (EmptySymTableException ex) {
            System.err.println("Unexpected EmptySymTableException " +
                               " in IdNode.nameAnalysis");
            System.exit(-1);
        } 
    }
 
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        if (mySym != null) {
            return mySym.getType();
        } 
        else {
            System.err.println("ID with null sym field in IdNode.typeCheck");
            System.exit(-1);
        }
        return null;
    }
        
    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
        if (mySym != null) {
            p.print("<" + mySym + ">");
        }
    }

    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
    private Sym mySym;
}

class IntLitNode extends ExpNode {
    public IntLitNode(int lineNum, int charNum, int intVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myIntVal = intVal;
    }

    public void codeGen() {
        Codegen.p.println("#Generate intval");
        Codegen.generate("li", Codegen.T0, this.myIntVal);
        Codegen.genPush(Codegen.T0);
    }
    
    /***
     * Return the line number for this literal.
     ***/
    public int lineNum() {
        return myLineNum;
    }
    
    /***
     * Return the char number for this literal.
     ***/
    public int charNum() {
        return myCharNum;
    }
        
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        return new IntegerType();
    }
    
    public void unparse(PrintWriter p, int indent) {
        p.print(myIntVal);
    }

    private int myLineNum;
    private int myCharNum;
    private int myIntVal;
}

class StrLitNode extends ExpNode {
    public StrLitNode(int lineNum, int charNum, String strVal) {
        myLineNum = lineNum;
        myCharNum = charNum;
        myStrVal = strVal;
    }

    public void codeGen() {
        String label = null;
        if(!stringMap.containsKey(myStrVal)) {
            Codegen.generate(".data");
            label = Codegen.nextLabel();
            Codegen.generateLabeled(label, ".asciiz ", "string literal", myStrVal);
            Codegen.generate(".text");
            stringMap.put(myStrVal, label);
        } else {
            label = stringMap.get(myStrVal);
        }
        Codegen.generate("la", Codegen.T0, label);
        Codegen.genPush(Codegen.T0);

    }
    
    /***
     * Return the line number for this literal.
     ***/
    public int lineNum() {
        return myLineNum;
    }
    
    /***
     * Return the char number for this literal.
     ***/
    public int charNum() {
        return myCharNum;
    }
    
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        return new StringType();
    }
        
    public void unparse(PrintWriter p, int indent) {
        p.print(myStrVal);
    }

    private int myLineNum;
    private int myCharNum;
    private String myStrVal;
}

class TupleAccessNode extends ExpNode {
    public TupleAccessNode(ExpNode loc, IdNode id) {
        myLoc = loc;	
        myId = id;
    }

    /***
     * Return the symbol associated with this colon-access node.
     ***/
    public Sym sym() {
        return mySym;
    }    
    
    /***
     * Return the line number for this colon-access node. 
     * The line number is the one corresponding to the RHS of the colon-access.
     ***/
    public int lineNum() {
        return myId.lineNum();
    }
    
    /***
     * Return the char number for this colon-access node.
     * The char number is the one corresponding to the RHS of the colon-access.
     ***/
    public int charNum() {
        return myId.charNum();
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, do:
     * - process the LHS of the colon-access
     * - process the RHS of the colon-access
     * - if the RHS is of a tuple type, set the sym for this node so that
     *   a colon-access "higher up" in the AST can get access to the symbol
     *   table for the appropriate tuple definition
     ***/
    public void nameAnalysis(SymTable symTab) {
        badAccess = false;
        SymTable tupleSymTab = null; // to lookup RHS of colon-access
        Sym sym = null;
        
        myLoc.nameAnalysis(symTab);  // do name analysis on LHS
        
        // if myLoc is really an ID, then sym will be a link to the ID's symbol
        if (myLoc instanceof IdNode) {
            IdNode id = (IdNode)myLoc;
            sym = id.sym();
            
            // check ID has been declared to be of a tuple type
            
            if (sym == null) { // ID was undeclared
                badAccess = true;
            }
            else if (sym instanceof TupleSym) { 
                // get symbol table for tuple type
                Sym tempSym = ((TupleSym)sym).getTupleType().sym();
                tupleSymTab = ((TupleDefSym)tempSym).getSymTable();
            } 
            else {  // LHS is not a tuple type
                ErrMsg.fatal(id.lineNum(), id.charNum(), 
                             "Colon-access of non-tuple type");
                badAccess = true;
            }
        }
        
        // if myLoc is really a colon-access (i.e., myLoc was of the form
        // LHSloc.RHSid), then sym will either be
        // null - indicating RHSid is not of a tuple type, or
        // a link to the Sym for the tuple type RHSid was declared to be
        else if (myLoc instanceof TupleAccessNode) {
            TupleAccessNode loc = (TupleAccessNode)myLoc;
            
            if (loc.badAccess) {  // if errors in processing myLoc
                badAccess = true; // don't continue proccessing this colon-access
            }
            else { //  no errors in processing myLoc
                sym = loc.sym();

                if (sym == null) {  // no tuple in which to look up RHS
                    ErrMsg.fatal(loc.lineNum(), loc.charNum(), 
                                 "Colon-access of non-tuple type");
                    badAccess = true;
                }
                else {  // get the tuple's symbol table in which to lookup RHS
                    if (sym instanceof TupleDefSym) {
                        tupleSymTab = ((TupleDefSym)sym).getSymTable();
                    }
                    else {
                        System.err.println("Unexpected Sym type in TupleAccessNode");
                        System.exit(-1);
                    }
                }
            }

        }
        
        else { // don't know what kind of thing myLoc is
            System.err.println("Unexpected node type in LHS of colon-access");
            System.exit(-1);
        }
        
        // do name analysis on RHS of colon-access in the tuple's symbol table
        if (!badAccess) {
			try {
				sym = tupleSymTab.lookupGlobal(myId.name()); // lookup
				if (sym == null) { // not found - RHS is not a valid field name
					ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
								"Invalid tuple field name");
					badAccess = true;
				}
            
				else {
					myId.link(sym);  // link the symbol
					// if RHS is itself as tuple type, link the symbol for its tuple 
					// type to this colon-access node (to allow chained colon-access)
					if (sym instanceof TupleSym) {
						mySym = ((TupleSym)sym).getTupleType().sym();
					}
				}
			} catch (EmptySymTableException ex) {
				System.err.println("Unexpected EmptySymTableException " +
								" in TupleAccessNode.nameAnalysis");
				System.exit(-1);
			} 
        }
    }    
 
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        return myId.typeCheck();
    }
        
    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myLoc.unparse(p, 0);
        p.print("):");
        myId.unparse(p, 0);
    }

    // 4 children
    private ExpNode myLoc;	
    private IdNode myId;
    private Sym mySym;          // link to Sym for tuple type
    private boolean badAccess;  // to prevent multiple, cascading errors
}

class AssignExpNode extends ExpNode {
    public AssignExpNode(ExpNode lhs, ExpNode exp) {
        myLhs = lhs;
        myExp = exp;
    }
    
    public void codeGen(){  
        // compute value of RHS expr and leave result on stack
        myExp.codeGen();
        // compute address of LHS location and leave result on stack
        ((IdNode)myLhs).genAddr();
        // pop RHS into $t1
        Codegen.genPop(Codegen.T0);
        // pop LHS into $t0
        Codegen.genPop(Codegen.T1);
        // store value in $t1 at address held in $t0
        Codegen.generateIndexed("sw",Codegen.T1, Codegen.T0,0);
        // push $t1 on stack
        Codegen.genPush(Codegen.T1);

    }
    /***
     * Return the line number for this assignment node. 
     * The line number is the one corresponding to the left operand.
     ***/
    public int lineNum() {
        return myLhs.lineNum();
    }
    
    /***
     * Return the char number for this assignment node.
     * The char number is the one corresponding to the left operand.
     ***/
    public int charNum() {
        return myLhs.charNum();
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's 
     * two children
     ***/
    public void nameAnalysis(SymTable symTab) {
        myLhs.nameAnalysis(symTab);
        myExp.nameAnalysis(symTab);
    }

    public void unparse(PrintWriter p, int indent) {
        if (indent != -1)  p.print("(");
        myLhs.unparse(p, 0);
        p.print(" = ");
        myExp.unparse(p, 0);
        if (indent != -1)  p.print(")");    
    }
  
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        Type typeLhs = myLhs.typeCheck();
        Type typeExp = myExp.typeCheck();
        Type retType = typeLhs;
        
        if (typeLhs.isFctnType() && typeExp.isFctnType()) {
            ErrMsg.fatal(lineNum(), charNum(), "Assignment to function name");
            retType = new ErrorType();
        }
        
        if (typeLhs.isTupleDefType() && typeExp.isTupleDefType()) {
            ErrMsg.fatal(lineNum(), charNum(), "Assignment to tuple name");
            retType = new ErrorType();
        }
        
        if (typeLhs.isTupleType() && typeExp.isTupleType()) {
            ErrMsg.fatal(lineNum(), charNum(), "Assignment to tuple variable");
            retType = new ErrorType();
        }        
        
        if (!typeLhs.equals(typeExp) && !typeLhs.isErrorType() && !typeExp.isErrorType()) {
            ErrMsg.fatal(lineNum(), charNum(), "Mismatched type");
            retType = new ErrorType();
        }
        
        if (typeLhs.isErrorType() || typeExp.isErrorType()) {
            retType = new ErrorType();
        }
        
        return retType;
    }
       
    // 2 children
    private ExpNode myLhs;
    private ExpNode myExp;
}

class CallExpNode extends ExpNode {
    public CallExpNode(IdNode name, ExpListNode elist) {
        myId = name;
        myExpList = elist;
    }

    public CallExpNode(IdNode name) {
        myId = name;
        myExpList = new ExpListNode(new LinkedList<ExpNode>());
    }


    public void codeGen(){
        // put argument values on the stack
        if(myExpList != null) {
            myExpList.codeGen();
        }
        // jump to callee preamble label 
        myId.codeGenJumpAndLink();
        Codegen.generate("add", Codegen.SP, this.myId.paramsSize());
        Codegen.genPush(Codegen.V0);
            // jal_fctnName (jump and link)

    } 
    /***
     * Return the line number for this call node. 
     * The line number is the one corresponding to the function name.
     ***/
    public int lineNum() {
        return myId.lineNum();
    }
    
    /***
     * Return the char number for this call node.
     * The char number is the one corresponding to the function name.
     ***/
    public int charNum() {
        return myId.charNum();
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's 
     * two children
     ***/
    public void nameAnalysis(SymTable symTab) {
        myId.nameAnalysis(symTab);
        myExpList.nameAnalysis(symTab);
    } 
      
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        if (!myId.typeCheck().isFctnType()) {  
            ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
                         "Call attempt on non-function");
            return new ErrorType();
        }
        
        FctnSym fctnSym = (FctnSym)(myId.sym());
        
        if (fctnSym == null) {
            System.err.println("null sym for Id in CallExpNode.typeCheck");
            System.exit(-1);
        }
        
        if (myExpList.size() != fctnSym.getNumParams()) {
            ErrMsg.fatal(myId.lineNum(), myId.charNum(), 
                         "Function call with wrong number of args");
            return fctnSym.getReturnType();
        }
        
        myExpList.typeCheck(fctnSym.getParamTypes());
        return fctnSym.getReturnType();
    }
         
    // **** unparse ****
    public void unparse(PrintWriter p, int indent) {
        myId.unparse(p, 0);
        p.print("(");
        if (myExpList != null) {
            myExpList.unparse(p, 0);
        }
        p.print(")");   
    }

    // 2 children
    private IdNode myId;
    private ExpListNode myExpList;  // possibly null
}

abstract class UnaryExpNode extends ExpNode {
    public UnaryExpNode(ExpNode exp) {
        myExp = exp;
    }
    
    /***
     * Return the line number for this unary expression node. 
     * The line number is the one corresponding to the  operand.
     ***/
    public int lineNum() {
        return myExp.lineNum();
    }
    
    /***
     * Return the char number for this unary expression node.
     * The char number is the one corresponding to the  operand.
     ***/
    public int charNum() {
        return myExp.charNum();
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's child
     ***/
    public void nameAnalysis(SymTable symTab) {
        myExp.nameAnalysis(symTab);
    }
    
    // 1 child
    protected ExpNode myExp;
}

abstract class BinaryExpNode extends ExpNode {
    public BinaryExpNode(ExpNode exp1, ExpNode exp2) {
        myExp1 = exp1;
        myExp2 = exp2;
    }
    
    /***
     * Return the line number for this binary expression node. 
     * The line number is the one corresponding to the left operand.
     ***/
    public int lineNum() {
        return myExp1.lineNum();
    }
    
    /***
     * Return the char number for this binary expression node.
     * The char number is the one corresponding to the left operand.
     ***/
    public int charNum() {
        return myExp1.charNum();
    }
    
    /***
     * nameAnalysis
     * Given a symbol table symTab, perform name analysis on this node's 
     * two children
     ***/
    public void nameAnalysis(SymTable symTab) {
        myExp1.nameAnalysis(symTab);
        myExp2.nameAnalysis(symTab);
    }
    
    // 2 children
    protected ExpNode myExp1;
    protected ExpNode myExp2;
}

// **********************************************************************
// ****  Subclasses of UnaryExpNode
// **********************************************************************

class NotNode extends UnaryExpNode {
    public NotNode(ExpNode exp) {
        super(exp);
    }

    public void codeGen() {
        myExp.codeGen();
        Codegen.genPop(Codegen.T0);
        Codegen.generate("li", Codegen.T1, 1);
        //Not by Xor with 1
        Codegen.generate("xor", Codegen.T0, Codegen.T0, Codegen.T1);
        Codegen.genPush(Codegen.T0);
    }

    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        Type type = myExp.typeCheck();
        Type retType = new LogicalType();
        
        if (!type.isErrorType() && !type.isLogicalType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                         "Logical operator used with non-logical operand");
            retType = new ErrorType();
        }
        
        if (type.isErrorType()) {
            retType = new ErrorType();
        }
        
        return retType;
    }

    

    public void unparse(PrintWriter p, int indent) {
        p.print("(~");
        myExp.unparse(p, 0);
        p.print(")");
    }
}

class UnaryMinusNode extends UnaryExpNode {
    public UnaryMinusNode(ExpNode exp) {
        super(exp);
    }

    public void codeGen() {
        myExp.codeGen();
        Codegen.genPop(Codegen.T0);
        //Load 0 for negation
        Codegen.generate("li", Codegen.T1, "0");
        //Subtract
        Codegen.generate("sub", Codegen.T1, Codegen.T1, Codegen.T0);
        //Push the Val
        Codegen.genPush(Codegen.T1);
    }

    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        Type type = myExp.typeCheck();
        Type retType = new IntegerType();
        
        if (!type.isErrorType() && !type.isIntegerType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                         "Arithmetic operator used with non-integer operand");
            retType = new ErrorType();
        }
        
        if (type.isErrorType()) {
            retType = new ErrorType();
        }
        
        return retType;
    }


    public void unparse(PrintWriter p, int indent) {
        p.print("(-");
        myExp.unparse(p, 0);
        p.print(")");
    }
}

// **********************************************************************
// ****  Subclasses of BinaryExpNode
// **********************************************************************

abstract class ArithmeticExpNode extends BinaryExpNode {
    public ArithmeticExpNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }
    
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        Type type1 = myExp1.typeCheck();
        Type type2 = myExp2.typeCheck();
        Type retType = new IntegerType();
        
        if (!type1.isErrorType() && !type1.isIntegerType()) {
            ErrMsg.fatal(myExp1.lineNum(), myExp1.charNum(),
                         "Arithmetic operator used with non-integer operand");
            retType = new ErrorType();
        }
        
        if (!type2.isErrorType() && !type2.isIntegerType()) {
            ErrMsg.fatal(myExp2.lineNum(), myExp2.charNum(),
                         "Arithmetic operator used with non-integer operand");
            retType = new ErrorType();
        }
        
        if (type1.isErrorType() || type2.isErrorType()) {
            retType = new ErrorType();
        }
        
        return retType;
    }
}

abstract class LogicalExpNode extends BinaryExpNode {
    public LogicalExpNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }
    
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        Type type1 = myExp1.typeCheck();
        Type type2 = myExp2.typeCheck();
        Type retType = new LogicalType();
        
        if (!type1.isErrorType() && !type1.isLogicalType()) {
            ErrMsg.fatal(myExp1.lineNum(), myExp1.charNum(),
                         "Logical operator used with non-logical operand");
            retType = new ErrorType();
        }
        
        if (!type2.isErrorType() && !type2.isLogicalType()) {
            ErrMsg.fatal(myExp2.lineNum(), myExp2.charNum(),
                         "Logical operator used with non-logical operand");
            retType = new ErrorType();
        }
        
        if (type1.isErrorType() || type2.isErrorType()) {
            retType = new ErrorType();
        }
        
        return retType;
    }
}

abstract class EqualityExpNode extends BinaryExpNode {
    public EqualityExpNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }
    
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        Type type1 = myExp1.typeCheck();
        Type type2 = myExp2.typeCheck();
        Type retType = new LogicalType();
        
        if (type1.isVoidType() && type2.isVoidType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                         "Equality operator used with void function calls");
            retType = new ErrorType();
        }
        
        if (type1.isFctnType() && type2.isFctnType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                         "Equality operator used with function names");
            retType = new ErrorType();
        }
        
        if (type1.isTupleDefType() && type2.isTupleDefType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                         "Equality operator used with tuple names");
            retType = new ErrorType();
        }
        
        if (type1.isTupleType() && type2.isTupleType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                         "Equality operator used with tuple variables");
            retType = new ErrorType();
        }        
        
        if (!type1.equals(type2) && !type1.isErrorType() && !type2.isErrorType()) {
            ErrMsg.fatal(lineNum(), charNum(),
                         "Mismatched type");
            retType = new ErrorType();
        }
        
        if (type1.isErrorType() || type2.isErrorType()) {
            retType = new ErrorType();
        }
        
        return retType;
    }
}

abstract class RelationalExpNode extends BinaryExpNode {
    public RelationalExpNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }
    
    /***
     * typeCheck
     ***/
    public Type typeCheck() {
        Type type1 = myExp1.typeCheck();
        Type type2 = myExp2.typeCheck();
        Type retType = new LogicalType();
        
        if (!type1.isErrorType() && !type1.isIntegerType()) {
            ErrMsg.fatal(myExp1.lineNum(), myExp1.charNum(),
                         "Relational operator used with non-integer operand");
            retType = new ErrorType();
        }
        
        if (!type2.isErrorType() && !type2.isIntegerType()) {
            ErrMsg.fatal(myExp2.lineNum(), myExp2.charNum(),
                         "Relational operator used with non-integer operand");
            retType = new ErrorType();
        }
        
        if (type1.isErrorType() || type2.isErrorType()) {
            retType = new ErrorType();
        }
        
        return retType;
    }
}

class PlusNode extends ArithmeticExpNode {
    public PlusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void codeGen() {
        // step 1: evaluate both operands
        myExp1.codeGen();
        myExp2.codeGen();
    
        // step 2: pop values in T0 and T1
        Codegen.genPop(Codegen.T1);
        Codegen.genPop(Codegen.T0);
        
        // step 3: do the addition (T0 = T0 + T1)
        Codegen.generate("add",  Codegen.T0, Codegen.T0,  Codegen.T1);
        
        // step 4: result
        Codegen.genPush( Codegen.T0);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" + ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class MinusNode extends ArithmeticExpNode {
    public MinusNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" - ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
    public void codeGen() {
        // step 1: evaluate both operands
        myExp1.codeGen();
        myExp2.codeGen();
    
        // step 2: pop values in T0 and T1
        Codegen.genPop(Codegen.T1);
        Codegen.genPop(Codegen.T0);
        
        // step 3: do the subtraction (T0 = T0 - T1)
        Codegen.generate("sub",  Codegen.T0, Codegen.T0,  Codegen.T1);
        
        // step 4: result
        Codegen.genPush( Codegen.T0);
    }
}

class TimesNode extends ArithmeticExpNode {
    public TimesNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" * ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen() {
        // step 1: evaluate both operands
        myExp1.codeGen();
        myExp2.codeGen();
    
        // step 2: pop values in T0 and T1
        Codegen.genPop(Codegen.T1);
        Codegen.genPop(Codegen.T0);
        
        // step 3: do the multiplication and move L0 to T0
        Codegen.generate("mult", Codegen.T0, Codegen.T1);
        Codegen.generate("mflo", Codegen.T0);
        // step 4: result
        Codegen.genPush( Codegen.T0);
    }
}

class DivideNode extends ArithmeticExpNode {
    public DivideNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" / ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen() {
        // step 1: evaluate both operands
        myExp1.codeGen();
        myExp2.codeGen();
    
        // step 2: pop values in T0 and T1
        Codegen.genPop(Codegen.T1);
        Codegen.genPop(Codegen.T0);
        
        // step 3: do the division and move L0 to T0
        Codegen.generate("div",  Codegen.T0, Codegen.T1);
        Codegen.generate("mflo", Codegen.T0); 
        // step 4: result
        Codegen.genPush( Codegen.T0);
    }
}

class EqualsNode extends EqualityExpNode {
    public EqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void codeGen() {
        // step 1: evaluate both operands
        myExp1.codeGen();
        myExp2.codeGen();
    
        // step 2: pop values in T0 and T1
        Codegen.genPop(Codegen.T1);
        Codegen.genPop(Codegen.T0);
        
        // step 3: do =
        Codegen.generate("seq",  Codegen.T0, Codegen.T0, Codegen.T1);
        // step 4: result
        Codegen.genPush(Codegen.T0);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" == ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class NotEqualsNode extends EqualityExpNode {
    public void codeGen() {
        // step 1: evaluate both operands
        myExp1.codeGen();
        myExp2.codeGen();
    
        // step 2: pop values in T0 and T1
        Codegen.genPop(Codegen.T1);
        Codegen.genPop(Codegen.T0);
        
        // step 3: do =
        Codegen.generate("sne",  Codegen.T0, Codegen.T0,  Codegen.T1);
        // step 4: result
        Codegen.genPush( Codegen.T0);
    }

    public NotEqualsNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" ~= ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class GreaterNode extends EqualityExpNode {
    public GreaterNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" > ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
    public void codeGen() {
        // step 1: evaluate both operands
        myExp1.codeGen();
        myExp2.codeGen();
    
        // step 2: pop values in T0 and T1
        Codegen.genPop(Codegen.T1);
        Codegen.genPop(Codegen.T0);
        
        // step 3: do > 
        Codegen.generate("sgt",  Codegen.T0, Codegen.T0,  Codegen.T1);
        // step 4: result
        Codegen.genPush( Codegen.T0);
    }
}

class GreaterEqNode extends EqualityExpNode {
    public GreaterEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" >= ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen() {
        // step 1: evaluate both operands
        myExp1.codeGen();
        myExp2.codeGen();
    
        // step 2: pop values in T0 and T1
        Codegen.genPop(Codegen.T1);
        Codegen.genPop(Codegen.T0);
        
        // step 3: do >= 
        Codegen.generate("sge",  Codegen.T0, Codegen.T0,  Codegen.T1);
        // step 4: result
        Codegen.genPush( Codegen.T0);
    }
}

class LessNode extends EqualityExpNode {
    public LessNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" < ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
    public void codeGen() {
        // step 1: evaluate both operands
        myExp1.codeGen();
        myExp2.codeGen();
    
        // step 2: pop values in T0 and T1
        Codegen.genPop(Codegen.T1);
        Codegen.genPop(Codegen.T0);
        
        // step 3: do <
        Codegen.generate("slt",  Codegen.T0, Codegen.T0,  Codegen.T1);
        // step 4: result
        Codegen.genPush( Codegen.T0);
    }
}

class LessEqNode extends EqualityExpNode {
    public LessEqNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" <= ");
        myExp2.unparse(p, 0);
        p.print(")");
    }

    public void codeGen() {
        // step 1: evaluate both operands
        myExp1.codeGen();
        myExp2.codeGen();
    
        // step 2: pop values in T0 and T1
        Codegen.genPop(Codegen.T1);
        Codegen.genPop(Codegen.T0);
        
        // step 3: do < 
        Codegen.generate("sle",  Codegen.T0, Codegen.T0,  Codegen.T1);
        // step 4: result
        Codegen.genPush( Codegen.T0);
    }
}

class AndNode extends LogicalExpNode {
    public AndNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void codeGen() {
        String skipCode = Codegen.nextLabel();
        String endLabel = Codegen.nextLabel();
        myExp1.codeGen();
        Codegen.genPop(Codegen.T0);
        Codegen.generate("li", Codegen.T1, "0");
        Codegen.generate("beq", Codegen.T1, Codegen.T0, skipCode);
        myExp2.codeGen();
        Codegen.genPop(Codegen.T0);
        Codegen.generate("li", Codegen.T1, "0");
        Codegen.generate("beq", Codegen.T1, Codegen.T0, skipCode);
        //Condition is True
        Codegen.generate("li", Codegen.T0, "1");
        Codegen.generate("b", endLabel);
        //Condition is false
        Codegen.genLabel(skipCode);
        Codegen.generate("li", Codegen.T0, "0");
        Codegen.genLabel(endLabel);

        Codegen.genPush(Codegen.T0);

    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" & ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

class OrNode extends LogicalExpNode {
    public OrNode(ExpNode exp1, ExpNode exp2) {
        super(exp1, exp2);
    }

    public void codeGen() {
        String runCode = Codegen.nextLabel();
        String skipCode = Codegen.nextLabel();
        String endLabel = Codegen.nextLabel();
        myExp1.codeGen();
        Codegen.genPop(Codegen.T0);
        Codegen.generate("li", Codegen.T1, "1");
        Codegen.generate("beq", Codegen.T1, Codegen.T0, runCode);
        myExp2.codeGen();
        Codegen.genPop(Codegen.T0);
        Codegen.generate("li", Codegen.T1, "0");
        Codegen.generate("beq", Codegen.T1, Codegen.T0, skipCode);
        //Condition is True
        Codegen.genLabel(runCode);
        Codegen.generate("li", Codegen.T0, "1");
        Codegen.generate("b", endLabel);
        //Condition is false
        Codegen.genLabel(skipCode);
        Codegen.generate("li", Codegen.T0, "0");
        Codegen.genLabel(endLabel);

        Codegen.genPush(Codegen.T0);

    }

    public void unparse(PrintWriter p, int indent) {
        p.print("(");
        myExp1.unparse(p, 0);
        p.print(" | ");
        myExp2.unparse(p, 0);
        p.print(")");
    }
}

