package org.jenkinsci.plugins.relution.json;

import java.util.List;

/**
 * Object to cast an specific JSON to-from it and override given values with the specified setters/getters.
 *
 * @author Christian Steiger
 */
public class APIObject {

    private String status;
    private String message;
    private Errors errors;
    private Integer total;
    private List<Results> results;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Errors getErrors() {
        return errors;
    }

    public void setErrors(Errors errors) {
        this.errors = errors;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public List<Results> getResults() {
        return results;
    }

    public void setResults(List<Results> results) {
        this.results = results;
    }

    public static class Errors {
    }

    public static class Results {

        private String uuid;
        private String type;
        private String internalName;
        private Object platforms;
        private Object categories;
        private List<Versions> versions;
        private String createdBy;
        private Long creationDate;
        private String modifiedBy;
        private Long modificationDate;
        private Integer rating;
        private Integer ratingCount;
        private Integer downloadCount;
        private ACL acl;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getInternalName() {
            return internalName;
        }

        public void setInternalName(String internalName) {
            this.internalName = internalName;
        }

        public Object getPlatforms() {
            return platforms;
        }

        public void setPlatforms(Object platforms) {
            this.platforms = platforms;
        }

        public Object getCategories() {
            return categories;
        }

        public void setCategories(Object categories) {
            this.categories = categories;
        }

        public List<Versions> getVersions() {
            return versions;
        }

        public void setVersions(List<Versions> versions) {
            this.versions = versions;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }

        public Long getCreationDate() {
            return creationDate;
        }

        public void setCreationDate(Long creationDate) {
            this.creationDate = creationDate;
        }

        public String getModifiedBy() {
            return modifiedBy;
        }

        public void setModifiedBy(String modifiedBy) {
            this.modifiedBy = modifiedBy;
        }

        public Long getModificationDate() {
            return modificationDate;
        }

        public void setModificationDate(Long modificationDate) {
            this.modificationDate = modificationDate;
        }

        public Integer getRating() {
            return rating;
        }

        public void setRating(Integer rating) {
            this.rating = rating;
        }

        public Integer getRatingCount() {
            return ratingCount;
        }

        public void setRatingCount(Integer ratingCount) {
            this.ratingCount = ratingCount;
        }

        public Integer getDownloadCount() {
            return downloadCount;
        }

        public void setDownloadCount(Integer downloadCount) {
            this.downloadCount = downloadCount;
        }
        
        public ACL getAcl() {
            return acl;
        }
        
        public void setAcl(ACL acl) {
            this.acl = acl;
        }
    }

    public static class Versions {

        private String uuid;
        private String appUuid;
        private String releaseStatus;
        private String versionName;
        private Integer versionCode;
        private Integer downloadCount;
        private Integer installCount;
        private String link;
        private File file;
        private Object screenshots;
        private Icon icon;
        private Description description;
        private Name name;
        private Keywords keywords;
        private Changelog changelog;
        private List<Constraints> constraints;
        private String copyright;
        private String developerName;
        private String developerWeb;
        private String developerEmail;
        private String createdBy;
        private String creationDate;
        private String modifiedBy;
        private Long modificationDate;
        private Integer rating;
        private Integer ratingCount;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getAppUuid() {
            return appUuid;
        }

        public void setAppUuid(String appUuid) {
            this.appUuid = appUuid;
        }

        public String getReleaseStatus() {
            return releaseStatus;
        }

        public void setReleaseStatus(String releaseStatus) {
            this.releaseStatus = releaseStatus;
        }

        public String getVersionName() {
            return versionName;
        }

        public void setVersionName(String versionName) {
            this.versionName = versionName;
        }

        public Integer getVersionCode() {
            return versionCode;
        }

        public void setVersionCode(Integer versionCode) {
            this.versionCode = versionCode;
        }

        public Integer getDownloadCount() {
            return downloadCount;
        }

        public void setDownloadCount(Integer downloadCount) {
            this.downloadCount = downloadCount;
        }

        public Integer getInstallCount() {
            return installCount;
        }

        public void setInstallCount(Integer installCount) {
            this.installCount = installCount;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public Object getScreenshots() {
            return screenshots;
        }

        public void setScreenshots(Object screenshots) {
            this.screenshots = screenshots;
        }

        public Icon getIcon() {
            return icon;
        }

        public void setIcon(Icon icon) {
            this.icon = icon;
        }

        public Description getDescription() {
            return description;
        }

        public void setDescription(Description description) {
            this.description = description;
        }

        public Name getName() {
            return name;
        }

        public void setName(Name name) {
            this.name = name;
        }

        public Keywords getKeywords() {
            return keywords;
        }

        public void setKeywords(Keywords keywords) {
            this.keywords = keywords;
        }

        public Changelog getChangelog() {
            return changelog;
        }

        public void setChangelog(Changelog changelog) {
            this.changelog = changelog;
        }

        public List<Constraints> getConstraints() {
            return constraints;
        }

        public void setConstraints(List<Constraints> constraints) {
            this.constraints = constraints;
        }

        public String getCopyright() {
            return copyright;
        }

        public void setCopyright(String copyright) {
            this.copyright = copyright;
        }

        public String getDeveloperName() {
            return developerName;
        }

        public void setDeveloperName(String developerName) {
            this.developerName = developerName;
        }

        public String getDeveloperWeb() {
            return developerWeb;
        }

        public void setDeveloperWeb(String developerWeb) {
            this.developerWeb = developerWeb;
        }

        public String getDeveloperEmail() {
            return developerEmail;
        }

        public void setDeveloperEmail(String developerEmail) {
            this.developerEmail = developerEmail;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }

        public String getCreationDate() {
            return creationDate;
        }

        public void setCreationDate(String creationDate) {
            this.creationDate = creationDate;
        }

        public String getModifiedBy() {
            return modifiedBy;
        }

        public void setModifiedBy(String modifiedBy) {
            this.modifiedBy = modifiedBy;
        }

        public Long getModificationDate() {
            return modificationDate;
        }

        public void setModificationDate(Long modificationDate) {
            this.modificationDate = modificationDate;
        }

        public Integer getRating() {
            return rating;
        }

        public void setRating(Integer rating) {
            this.rating = rating;
        }

        public Integer getRatingCount() {
            return ratingCount;
        }

        public void setRatingCount(Integer ratingCount) {
            this.ratingCount = ratingCount;
        }
    }
    
    public static class File {
        private String uuid;
        private String name;
        private String link;
        private String contentType;
        private Integer size;
        private Long modificationDate;
        private String downloadCount;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public Long getModificationDate() {
            return modificationDate;
        }

        public void setModificationDate(Long modificationDate) {
            this.modificationDate = modificationDate;
        }

        public String getDownloadCount() {
            return downloadCount;
        }

        public void setDownloadCount(String downloadCount) {
            this.downloadCount = downloadCount;
        }
    }
    
    public static class Icon {
        private String uuid;
        private String name;
        private String link;
        private String contentType;
        private Integer size;
        private Long modificationDate;
        private String downloadCount;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public Long getModificationDate() {
            return modificationDate;
        }

        public void setModificationDate(Long modificationDate) {
            this.modificationDate = modificationDate;
        }

        public String getDownloadCount() {
            return downloadCount;
        }

        public void setDownloadCount(String downloadCount) {
            this.downloadCount = downloadCount;
        }
    }
    
    public static class Description {
        private String en_US;
        private String de_DE;

        public String getEn_US() {
            return en_US;
        }

        public void setEn_US(String en_US) {
            this.en_US = en_US;
        }

        public String getDe_DE() {
            return de_DE;
        }

        public void setDe_DE(String de_DE) {
            this.de_DE = de_DE;
        }
    }
    
    public static class Name {
        private String en_US;
        private String de_DE;

        public String getEn_US() {
            return en_US;
        }

        public void setEn_US(String en_US) {
            this.en_US = en_US;
        }

        public String getDe_DE() {
            return de_DE;
        }

        public void setDe_DE(String de_DE) {
            this.de_DE = de_DE;
        }
    }
    
    public static class Keywords {
        private String en_US;
        private String de_DE;

        public String getEn_US() {
            return en_US;
        }

        public void setEn_US(String en_US) {
            this.en_US = en_US;
        }

        public String getDe_DE() {
            return de_DE;
        }

        public void setDe_DE(String de_DE) {
            this.de_DE = de_DE;
        }
    }
    
    public static class Changelog {
        private String en_US;
        private String de_DE;

        public String getEn_US() {
            return en_US;
        }

        public void setEn_US(String en_US) {
            this.en_US = en_US;
        }

        public String getDe_DE() {
            return de_DE;
        }

        public void setDe_DE(String de_DE) {
            this.de_DE = de_DE;
        }
    }
    
    public static class Constraints {
        private String uuid;
        private String name;
        private String value;
        private String type;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
    
    public static class ACL {
        private List<String> RELEASE;
        private List<String> DEVELOPMENT;
        private List<String> REVIEW;
        private List<String> ARCHIV;

        public List<String> getRELEASE() {
            return RELEASE;
        }

        public void setRELEASE(List<String> RELEASE) {
            this.RELEASE = RELEASE;
        }

        public List<String> getDEVELOPMENT() {
            return DEVELOPMENT;
        }

        public void setDEVELOPMENT(List<String> DEVELOPMENT) {
            this.DEVELOPMENT = DEVELOPMENT;
        }

        public List<String> getREVIEW() {
            return REVIEW;
        }

        public void setREVIEW(List<String> REVIEW) {
            this.REVIEW = REVIEW;
        }

        public List<String> getARCHIV() {
            return ARCHIV;
        }

        public void setARCHIV(List<String> ARCHIV) {
            this.ARCHIV = ARCHIV;
        }
        
    }
}
