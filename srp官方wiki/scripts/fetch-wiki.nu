const API_URL = "https://scape-and-run-parasites.fandom.com/api.php"
const SITE_URL = "https://scape-and-run-parasites.fandom.com/wiki/"

def safe-filename [title: string] {
    $title
    | str replace --all --regex '[<>:"/\\|?*]' '_'
    | str replace --all --regex '[. ]+$' ''
}

def get-json [url: string] {
    mut attempt = 0

    loop {
        $attempt += 1
        let response = (try { http get $url } catch { null })

        if $response != null {
            return $response
        }

        if $attempt >= 3 {
            error make {msg: $"Request failed after 3 attempts: ($url)"}
        }

        sleep 2sec
    }
}

def list-pages [namespace: int] {
    mut pages = []
    mut continuation = {}

    loop {
        mut params = {
            action: "query"
            list: "allpages"
            apnamespace: $namespace
            aplimit: "max"
            format: "json"
            formatversion: 2
        }

        if ($continuation | is-not-empty) {
            $params = ($params | merge $continuation)
        }

        let query = ($params | url build-query)
        let response = (get-json $"($API_URL)?($query)")
        $pages = ($pages | append $response.query.allpages)

        if "continue" not-in $response {
            break
        }

        $continuation = ($response | get "continue")
    }

    $pages
}

def fetch-namespace [namespace: int, directory: string] {
    let pages = (list-pages $namespace)
    let pages_with_content = ($pages | chunks 50 | each {|batch|
        let params = {
            action: "query"
            pageids: ($batch.pageid | str join "|")
            prop: "revisions"
            rvprop: "content"
            rvslots: "main"
            format: "json"
            formatversion: 2
        }
        let query = ($params | url build-query)
        let response = (get-json $"($API_URL)?($query)")
        $response.query.pages
    } | flatten)

    mkdir $directory

    $pages_with_content | each {|page|
        let safe_title = (safe-filename $page.title)
        let filename = $"($page.pageid)_($safe_title).wiki"
        let path = ($directory | path join $filename)
        let content = ($page.revisions.0.slots.main.content? | default "")
        $content | save --force $path

        {
            pageid: $page.pageid
            namespace: $namespace
            title: $page.title
            source_url: $"($SITE_URL)($page.title | url encode)"
            local_path: ($path | str replace --all "\\" "/")
            bytes: ($content | encode utf-8 | bytes length)
        }
    }
}

let articles = (fetch-namespace 0 "wiki/articles")
let categories = (fetch-namespace 14 "wiki/categories")
let manifest = ($articles | append $categories | sort-by namespace title)

$manifest | save --force wiki/manifest.csv

{
    fetched_at: (date now | date to-timezone "UTC" | format date "%Y-%m-%dT%H:%M:%SZ")
    source: $SITE_URL
    article_pages: ($articles | length)
    category_pages: ($categories | length)
    total_pages: ($manifest | length)
} | to json --indent 2 | save --force wiki/metadata.json

print $"Downloaded ($articles | length) article pages and ($categories | length) category pages."
