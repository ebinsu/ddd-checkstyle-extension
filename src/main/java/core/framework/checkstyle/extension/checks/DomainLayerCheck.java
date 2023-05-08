package core.framework.checkstyle.extension.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.CommonUtil;
import com.puppycrawl.tools.checkstyle.utils.TokenUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ebin
 */
public class DomainLayerCheck extends AbstractCheck {
    private String applicationLayer = "application";
    private String domainLayer = "domain";
    private String infrastructureLayer = "infrastructure";
    private String userInterfaceLayer = "interface";

    private Pattern domainDirPathPattern;
    private Pattern domainPkgPattern;
    private Pattern anotherLayerPkgPattern;

    private String currentDomainPackage;
    private boolean processCurrentFile;
    private Set<String> aggDomainPackages = new HashSet<>();

    @Override
    public void init() {
        super.init();
        domainDirPathPattern = Pattern.compile("^.*[\\\\/]" + domainLayer + "\\.?");
        domainPkgPattern = Pattern.compile(".*\\." + domainLayer);
        anotherLayerPkgPattern = Pattern.compile(".*\\.(" + infrastructureLayer + "|" + userInterfaceLayer + "|" + applicationLayer + ").*");
    }

    @Override
    public int[] getDefaultTokens() {
        return getAcceptableTokens();
    }

    @Override
    public int[] getAcceptableTokens() {
        return new int[]{
                TokenTypes.IMPORT,
                TokenTypes.ANNOTATION
        };
    }

    @Override
    public int[] getRequiredTokens() {
        return CommonUtil.EMPTY_INT_ARRAY;
    }

    @Override
    public void beginTree(DetailAST root) {
        this.processCurrentFile = domainDirPathPattern.matcher(getFileContents().getFileName()).find();
        if (this.processCurrentFile) {
            this.currentDomainPackage = TokenUtil
                    .findFirstTokenByPredicate(root, r -> TokenTypes.PACKAGE_DEF == r.getType())
                    .map(pkgDef -> {
                        String fullPkg = FullIdent.createFullIdent(pkgDef.getFirstChild().getNextSibling()).getText();
                        Matcher matcher = domainPkgPattern.matcher(fullPkg);
                        if (matcher.find()) {
                            return matcher.group();
                        }
                        return fullPkg;
                    })
                    .orElse(null);
            if (this.currentDomainPackage == null) {
                this.processCurrentFile = false;
            }
        }
    }

    @Override
    public void visitToken(DetailAST ast) {
        if (!this.processCurrentFile) {
            return;
        }
        if (TokenTypes.IMPORT == ast.getType()) {
            DetailAST startingDot = ast.getFirstChild();
            FullIdent name = FullIdent.createFullIdent(startingDot);
            String importText = name.getText();
            if (anotherLayerPkgPattern.matcher(importText).matches()) {
                log(ast.getLineNo(), ast.getColumnNo(), "domain.layer.pkgs.disallow");
            } else if (domainPkgPattern.matcher(importText).find() && !importText.contains(currentDomainPackage)) {
                log(ast.getLineNo(), ast.getColumnNo(), "domain.entities.pkgs.disallow");
            }
        } else if (TokenTypes.ANNOTATION == ast.getType()) {
            String annotationText = FullIdent.createFullIdent(ast.getLastChild()).getText();
            if ("AggregateRoot".equals(annotationText)) {
                if (!aggDomainPackages.add(this.currentDomainPackage)) {
                    log(ast.getLineNo(), ast.getColumnNo(), "domain.multiple.agg.disallow");
                }
            }
        }
    }
}
