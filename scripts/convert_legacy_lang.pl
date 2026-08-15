#!/usr/bin/env perl
use strict;
use warnings;
use utf8;
use open qw(:std :encoding(UTF-8));
use JSON::PP;

die "usage: convert_legacy_lang.pl CURRENT_EN LEGACY_EN LEGACY_LOCALE EXISTING_LOCALE OUTPUT [keep-existing-fallback]\n"
    unless @ARGV == 5 || @ARGV == 6;

my ($current_en_path, $legacy_en_path, $legacy_locale_path, $existing_path, $output_path,
        $keep_existing_fallback) = @ARGV;

sub read_json {
    my ($path) = @_;
    open my $file, '<:encoding(UTF-8)', $path or die "cannot read $path: $!\n";
    local $/;
    return JSON::PP->new->utf8(0)->decode(<$file>);
}

sub read_lang {
    my ($path) = @_;
    open my $file, '<:encoding(UTF-8)', $path or die "cannot read $path: $!\n";
    my %entries;
    while (my $line = <$file>) {
        $line =~ s/\R\z//;
        next if $line =~ /^\s*(?:#|$)/;
        my ($key, $value) = split /=/, $line, 2;
        next unless defined $value;
        $key =~ s/^\s+|\s+$//g;
        $value =~ s/\\n/\n/g;
        $value =~ s/\\t/\t/g;
        $entries{$key} = $value;
    }
    return \%entries;
}

sub normalized_value {
    my ($value) = @_;
    $value =~ s/§[0-9a-fk-or]//ig;
    $value =~ s/\s+/ /g;
    $value =~ s/^\s+|\s+$//g;
    return lc $value;
}

sub direct_candidates {
    my ($key) = @_;
    my @keys;
    my $legacy = $key;
    $legacy =~ s/\.csrp\./.srparasites./g;
    push @keys, $legacy;
    push @keys, "$legacy.name" if $key =~ /^(?:entity|item)\.csrp\./;
    if ($key =~ /^block\.csrp\.(.+)$/) {
        push @keys, "tile.srparasites.$1.name", "tile.srparasites.$1";
    }
    if ($key =~ /^effect\.csrp\.(.+)$/) {
        push @keys, "mob_effect.srparasites.$1", "effect.srparasites.$1";
    }
    return @keys;
}

sub key_score {
    my ($current, $legacy) = @_;
    my $score = 0;
    my ($prefix) = split /\./, $current;
    $score += 20 if $legacy =~ /^\Q$prefix\E\./;
    $score += 20 if $prefix =~ /^advancements?$/ && $legacy =~ /^advancements?\./;
    $score += 20 if $prefix eq 'tooltip' && $legacy =~ /^toot?ip\./;
    $score += 20 if $prefix eq 'block' && $legacy =~ /^tile\./;
    $score += 20 if $prefix eq 'effect' && $legacy =~ /^(?:mob_effect|effect)\./;
    my ($id) = $current =~ /^[^.]+\.csrp\.([^.]+)/;
    $score += 50 if defined $id && $legacy =~ /(?:^|[._])\Q$id\E(?:[._]|$)/;
    my %ignored = map { $_ => 1 } qw(
        csrp srparasites item entity block tile effect gui screen report options
        name title description desc tooltip tootip subtitles advancement advancements
    );
    my %current_tokens = map { lc($_) => 1 }
            grep { length($_) >= 4 && !$ignored{lc($_)} } split /[._]/, $current;
    for my $token (grep { length($_) >= 4 && !$ignored{lc($_)} } split /[._]/, $legacy) {
        $score += 5 if $current_tokens{lc $token};
    }
    return $score;
}

my $current_en = read_json($current_en_path);
my $legacy_en = read_lang($legacy_en_path);
my $legacy_locale = read_lang($legacy_locale_path);
my $existing = -f $existing_path ? read_json($existing_path) : {};
my %preserve_existing = map { $_ => 1 } qw(
    itemGroup.csrp
    item.csrp.fog_bottle
    block.csrp.deadhead_leaves
    block.csrp.infested_button
    block.csrp.infested_pressure_plate
    block.csrp.infested_ladder
    block.csrp.infested_bookshelf
    block.csrp.cooked_flesh_button
    block.csrp.cooked_flesh_pressure_plate
    block.csrp.cooked_flesh_ladder
    block.csrp.cooked_flesh_bookshelf
);

my %english_to_keys;
for my $key (keys %$legacy_en) {
    push @{$english_to_keys{normalized_value($legacy_en->{$key})}}, $key;
}

my %result;
my ($direct_matches, $value_matches, $fallbacks, $unsafe_fallbacks, $preserved) = (0, 0, 0, 0, 0);
for my $key (keys %$current_en) {
    if ($preserve_existing{$key} && exists $existing->{$key}) {
        $result{$key} = $existing->{$key};
        $preserved++;
        next;
    }

    my $matched_key;
    for my $candidate (direct_candidates($key)) {
        if (exists $legacy_locale->{$candidate}) {
            $matched_key = $candidate;
            last;
        }
    }
    if (defined $matched_key) {
        $result{$key} = $legacy_locale->{$matched_key};
        $direct_matches++;
        next;
    }

    my @candidates = grep { exists $legacy_locale->{$_} }
            @{$english_to_keys{normalized_value($current_en->{$key})} // []};
    if (@candidates) {
        @candidates = sort { key_score($key, $b) <=> key_score($key, $a) } @candidates;
        if (key_score($key, $candidates[0]) > 0) {
            $result{$key} = $legacy_locale->{$candidates[0]};
            $value_matches++;
        } else {
            $result{$key} = $keep_existing_fallback && exists $existing->{$key}
                    ? $existing->{$key} : $current_en->{$key};
            $unsafe_fallbacks++;
        }
    } else {
        $result{$key} = $keep_existing_fallback && exists $existing->{$key}
                ? $existing->{$key} : $current_en->{$key};
        $fallbacks++;
    }
}

if ($keep_existing_fallback) {
    for my $key (keys %$existing) {
        $result{$key} = $existing->{$key} unless exists $result{$key};
    }
}

my $placeholder_fallbacks = 0;
for my $key (keys %result) {
    next unless exists $current_en->{$key};
    my @expected = $current_en->{$key} =~ /%(?:\d+\$)?[a-z]/ig;
    my @actual = $result{$key} =~ /%(?:\d+\$)?[a-z]/ig;
    if (@expected != @actual) {
        $result{$key} = $current_en->{$key};
        $placeholder_fallbacks++;
    }
}

open my $output, '>:encoding(UTF-8)', $output_path or die "cannot write $output_path: $!\n";
my $json = JSON::PP->new->canonical(1)->pretty(1)->indent_length(2)->space_before(0)->space_after(1);
print {$output} $json->encode(\%result);
close $output;

print "preserved=$preserved direct=$direct_matches value=$value_matches fallback=$fallbacks total="
        . scalar(keys %result) . " unsafe_fallback=$unsafe_fallbacks"
        . " placeholder_fallback=$placeholder_fallbacks\n";
