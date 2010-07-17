package abs.frontend.typesystem;

import static abs.common.StandardLib.STDLIB_DATATYPES_STRING;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import abs.frontend.FrontendTest;
import abs.frontend.ast.ClassDecl;
import abs.frontend.ast.FieldDecl;
import abs.frontend.ast.FunctionDecl;
import abs.frontend.ast.Model;
import abs.frontend.ast.ParametricFunctionDecl;
import abs.frontend.ast.ReturnStmt;
import abs.frontend.ast.TypeParameterDecl;
import abs.frontend.typechecker.DataTypeType;
import abs.frontend.typechecker.Type;
import abs.frontend.typechecker.TypeParameter;
import abs.frontend.typechecker.UnionType;

public class TypingTest extends FrontendTest {

    @Test
    public void testContextDecl() {
        Model m = assertParseOk("class C implements I { I m() { return this; } } interface I { }");
        ClassDecl d = (ClassDecl)m.getDecl(0);
        ReturnStmt s = (ReturnStmt) d.getMethod(0).getBlock().getStmt(0);
        assertEquals(d,s.getRetExp().getContextDecl());
    }
    
    @Test
    public void testThisTyping() {
        Model m = assertParseOk("class C implements I { I m() { return this; } } interface I { }");
        ClassDecl d = (ClassDecl)m.getDecl(0);
        ReturnStmt s = (ReturnStmt) d.getMethod(0).getBlock().getStmt(0);
        assertEquals(m.getDecl(1),((UnionType)s.getRetExp().getType()).getType(0).getDecl());
        
    }
    
    @Test
    public void testInterfaceType() {
        Model m = assertParseOk("interface I { } { I i; i = i; }");
        assertEquals(m.localLookup("I").getType(),getTypeOfFirstAssignment(m));
        
    }
    
    @Test
    public void testDataTypeBoolLit() {
        Model m = assertParseOkStdLib("{ Bool i; i = True; }");
        assertEquals(m.getBoolType(),getTypeOfFirstAssignment(m));
    }
    
    @Test
    public void testDataTypeIntLit() {
        Model m = assertParseOkStdLib("{ Int i; i = 5; }");
        assertEquals(m.getIntType(),getTypeOfFirstAssignment(m));
    }

    @Test
    public void testDataTypeStringLit() {
        Model m = assertParseOkStdLib("{ String i; i = \"5\"; }");
        assertEquals(m.getStringType(),getTypeOfFirstAssignment(m));
    }
    
    @Test
    public void testLetExp() {
   	 Model m = assertParseOkStdLib("def Bool f() = let (Bool b) = True in b;");
        assertEquals(m.getBoolType(),getFirstFunctionExpr(m).getType());
    }

    @Test
    public void testCase() {
   	 Model m = assertParseOkStdLib("def Bool f(Bool x) = case x { True => False; False => True; };");
        assertEquals(m.getBoolType(),getFirstFunctionExpr(m).getType());
    }

    @Test
    public void testFnApp() {
   	   Model m = assertParseOkStdLib("def Bool f() = f();");
       assertEquals(m.getBoolType(),getFirstFunctionExpr(m).getType());
    }


    @Test
    public void testNew() {
        Model m = assertParseOk("interface I {} class C implements I {} { I i; i = new C(); }");
        assertEquals(m.localLookup("I").getType(),((UnionType)getFirstExp(m).getType()).getType(0));
    }
    
    @Test
    public void testFieldUse() {
        Model m = assertParseOkStdLib(" class C { Bool f; Bool m() { return this.f; } }");
        ClassDecl d = (ClassDecl) m.localLookup("C");
        FieldDecl f = d.getField(0);
        ReturnStmt s = (ReturnStmt) d.getMethod(0).getBlock().getStmt(0);
        assertEquals(f.getType(), s.getRetExp().getType());
    }
    
    @Test
    public void testSyncCall() {
        Model m = assertParseOkStdLib(" interface I { Bool m(); } { I i; i.m(); }");
        assertEquals(m.getBoolType(), getFirstExp(m).getType());
    }
    
    @Test
    public void testAsyncCall() {
        Model m = assertParseOkStdLib(" interface I { Bool m(); } { I i; i!m(); }");
        assertEquals(m.getFutType(m.getBoolType()), getFirstExp(m).getType());
    }

    @Test
    public void functionTypeParams() {
        Model m = assertParseOkStdLib(" def A f<A>(A a) = a ;");
        ParametricFunctionDecl d = getFirstParametricFunctionDecl(m);
        assertEquals(d.getTypeParameter(0), ((TypeParameter)d.getFunDef().getType()).getDecl());
    }

    @Test
    public void functionTypeArgs() {
        Model m = assertParseOkStdLib(" def Opt<A> f<A>() = None ;");
        ParametricFunctionDecl d = getFirstParametricFunctionDecl(m);
        DataTypeType t = (DataTypeType) d.getTypeUse().getType();
        TypeParameter typeArg = (TypeParameter) t.getTypeArg(0);
        assertEquals(typeArg.getDecl(), d.getTypeParameter(0));
    }
    
    @Test
    public void functionTypeArgs2() {
        Model m = assertParseOkStdLib(" def Opt<A> f<A>(Opt<A> o) = o ;");
        ParametricFunctionDecl d = getFirstParametricFunctionDecl(m);
        assertEquals(d.getTypeUse().getType(), d.getFunDef().getType());
    }

    @Test
    public void functionTypeArgs3() {
        Model m = assertParseOkStdLib(" def A f<A>(Opt<A> o) = case o { Some(a) => a; } ;");
        ParametricFunctionDecl d = getFirstParametricFunctionDecl(m);
        TypeParameterDecl typeParameter = d.getTypeParameter(0);
        TypeParameter type = (TypeParameter)d.getFunDef().getType();
        TypeParameterDecl decl = type.getDecl();
        assertEquals(typeParameter, decl);
    }

    @Test
    public void functionTypeArgs4() {
        Model m = assertParseOkStdLib(" data Foo<A> = Bar(A,Bool); " +
        		"def Bool f<A>(Foo<A> o) = case o { Bar(a,b) => b; } ;");
        
        ParametricFunctionDecl d = getFirstParametricFunctionDecl(m);
        Type type = d.getFunDef().getType();
        assertEquals(m.getBoolType(), type);
    }
    
}
