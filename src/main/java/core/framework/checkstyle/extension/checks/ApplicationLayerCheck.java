package core.framework.checkstyle.extension.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.CommonUtil;

import java.util.regex.Pattern;

/**
 * @author ebin
 */
public class ApplicationLayerCheck extends AbstractCheck {
    private String applicationLayer = "application";
    private String infrastructureLayer = "infrastructure";
    private String userInterfaceLayer = "interface";

    private Pattern applicationDirPathPattern;
    private Pattern anotherLayerPkgPattern;

    private boolean processCurrentFile;

    @Override
    public void init() {
        super.init();
        applicationDirPathPattern = Pattern.compile("^.*[\\\\/]" + applicationLayer + "\\.?");
        anotherLayerPkgPattern = Pattern.compile(".*\\.(" + infrastructureLayer + "|" + userInterfaceLayer + ").*");
    }

    @Override
    public int[] getDefaultTokens() {
        return getAcceptableTokens();
    }

    @Override
    public int[] getAcceptableTokens() {
        return new int[]{
                TokenTypes.IMPORT
        };
    }

    @Override
    public int[] getRequiredTokens() {
        return CommonUtil.EMPTY_INT_ARRAY;
    }

    @Override
    public void beginTree(DetailAST root) {
        this.processCurrentFile = applicationDirPathPattern.matcher(getFileContents().getFileName()).find();
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
                log(ast.getLineNo(), ast.getColumnNo(), "application.layer.pkgs.disallow");
            }
        }
    }
}
