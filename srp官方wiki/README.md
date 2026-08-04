# Scape and Run: Parasites Wiki 本地归档

本仓库保存 [Scape and Run: Parasites Wiki](https://scape-and-run-parasites.fandom.com/wiki/Scape_and_Run:_Parasites_Wiki) 的页面源码。

## 目录

- `wiki/articles/`：主命名空间（namespace 0）的全部文章与重定向页。
- `wiki/categories/`：分类命名空间（namespace 14）的全部分类说明页。
- `wiki/manifest.csv`：页面 ID、标题、原始 URL、本地路径与内容长度。
- `wiki/metadata.json`：抓取时间、来源和页面数量。
- `scripts/fetch-wiki.nu`：通过 Fandom MediaWiki API 重新抓取全部页面的 Nushell 脚本。

页面以 MediaWiki 原生 wikitext 保存，扩展名为 `.wiki`。文件名以稳定的 `pageid` 开头，并将 Windows 不支持的文件名字符替换为下划线。原生格式可以完整保留模板参数、表格、分类和重定向信息。

## 更新归档

```nu
nu scripts/fetch-wiki.nu
```

脚本会覆盖同一页面的已有文件，并重新生成清单和元数据。
