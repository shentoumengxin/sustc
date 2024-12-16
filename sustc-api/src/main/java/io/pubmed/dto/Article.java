package io.pubmed.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The article information class
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Article implements Serializable {
    /**
     * Article's main id, pmid.
     */
    private int id;

    /**
     * The article's authors.
     */
    private Author[] authors;

    /**
     * The article's title.
     */
    private String title;

    /**
     * The article's keywords.
     */
    private String[] keywords;

    /**
     * Journal of this article.
     */
    private Journal journal;

    /**
     * List of article's references to other articles.
     */
    private String[] references;

    private ArticleID[] article_ids;

    private PublicationType[] publication_types;

    /**
     * Grants awarded to this article.
     */
    private Grant[] grants;

    /**
     * date_created
     */
    private Date created;

    /**
     * date_completed
     */
    private Date completed;

    /**
     * pub_model of this article.
     */
    private String pub_model;

    public int getId() {
        return id;
    }

    public Author[] getAuthors() {
        return authors;
    }

    public String getTitle() {
        return title;
    }

    public String[] getKeywords() {
        return keywords;
    }

    public Journal getJournal() {
        return journal;
    }

    public String[] getReferences() {
        return references;
    }

    public ArticleID[] getArticle_ids() {
        return article_ids;
    }

    public PublicationType[] getPublication_types() {
        return publication_types;
    }

    public Grant[] getGrants() {
        return grants;
    }

    public Date getCreated() {
        return created;
    }

    public Date getCompleted() {
        return completed;
    }

    public String getPub_model() {
        return pub_model;
    }
}
