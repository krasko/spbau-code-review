package net.ldvsoft.warofviruses;

/**
 * Created by ldvsoft on 11.12.15.
 */
public class User {
    private long id;
    private String googleToken;
    private String nickNameStr;
    private int nickNameId;
    private int colorCross;
    private int colorZero;
    private User invitationTarget;

    public User(
            long id,
            String googleToken,
            String nickNameStr, int nickNameId,
            int colorCross, int colorZero,
            User invitationTarget) {
        this.id = id;
        this.googleToken = googleToken;
        this.nickNameStr = nickNameStr;
        this.nickNameId = nickNameId;
        this.colorCross = colorCross;
        this.colorZero = colorZero;
        this.invitationTarget = invitationTarget;
    }

    public String getFullNickname() {
        return nickNameStr + "#" + nickNameId;
    }

    public long getId() {
        return id;
    }

    public String getGoogleToken() {
        return googleToken;
    }

    public String getNickNameStr() {
        return nickNameStr;
    }

    public int getNickNameId() {
        return nickNameId;
    }

    public int getColorCross() {
        return colorCross;
    }

    public int getColorZero() {
        return colorZero;
    }

    public void setNickNameStr(String nickname) {
        this.nickNameStr = nickname;
    }

    public void setCrossColor(int crossColor) {
        this.colorCross = crossColor;
    }

    public void setZeroColor(int zeroColor) {
        this.colorZero = zeroColor;
    }
}
